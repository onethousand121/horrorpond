package com.horrorpond.ingestion.application;

import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.ItchClient;
import com.horrorpond.ingestion.client.ItchTransientException;
import com.horrorpond.ingestion.client.ItchUrls;
import com.horrorpond.ingestion.domain.ItchDiscoveredBy;
import com.horrorpond.ingestion.domain.ItchGameSeed;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.ItchGameSeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * itch.io 수집. 공포 태그는 짧은 게임잼 작품이 너무 많아 전부 받지 않는다.
 * <ol>
 *   <li>발견: 공포 태그 평점순 목록에서 평가 수가 itch.min-ratings 이상인 게임만 seed로 남긴다.
 *       이미 아는 게임은 목록에서 본 평가 수로 바로 갱신한다(게임 페이지를 받지 않아도 매일 기록이 쌓인다).</li>
 *   <li>상세: 처음 받는 게임과 갱신 주기가 지난 게임의 페이지를 받아 Game에 반영한다.</li>
 * </ol>
 * 관리자가 주소로 추가한 게임({@link #addManual})은 기준과 상관없이 바로 받는다.
 * HTTP 호출과 대기는 트랜잭션 밖에서 하고, 저장은 {@link ItchWriter}가 1건씩 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItchIngestionService {

    private final ItchClient client;
    private final ItchPageParser parser;
    private final ItchWriter writer;
    private final ItchGameSeedRepository seedRepository;
    private final IngestionJobRecorder jobRecorder;
    private final IngestionProperties properties;
    private final Clock clock;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.ITCH, trigger);
        try {
            int discovered = discover();
            Outcome outcome = fetchPages(jobId);
            log.info("itch.io ingestion finished: discovered={}, fetched={}, failed={}", discovered,
                    outcome.processed(), outcome.failed());
            jobRecorder.succeed(jobId, outcome.processed(), outcome.failed());
        } catch (RuntimeException e) {
            log.error("itch.io ingestion failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    /**
     * 관리자가 고른 게임을 지금 받는다 (공포 태그·평가 수 기준 없음).
     *
     * @return 게임 id
     */
    public long addManual(String rawUrl) {
        String url = ItchUrls.normalize(rawUrl);
        String html;
        try {
            html = client.fetchGamePage(url);
        } catch (ItchTransientException e) {
            log.warn("itch.io page fetch failed: {}", e.getMessage());
            throw new DomainStateException("itch.io에서 게임 페이지를 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (html == null) {
            throw new NotFoundException("itch.io game not found: " + url);
        }
        ParsedItchGame parsed;
        try {
            parsed = parser.parseGame(html);
        } catch (ItchPageParseException e) {
            throw new DomainValidationException("itch.io 게임 페이지가 아닙니다: " + url);
        }
        return Objects.requireNonNull(writer.apply(url, parsed, ItchDiscoveredBy.MANUAL));
    }

    /**
     * 평점순 목록을 앞에서부터 본다. 목록을 못 받으면 거기서 멈추고 상세 단계로 넘어간다.
     *
     * @return 새로 발견한 게임 수
     */
    int discover() {
        IngestionProperties.Itch config = properties.itch();
        int discovered = 0;
        for (int page = 1; page <= config.maxPages(); page++) {
            List<ItchListing> listings;
            try {
                listings = parser.parseListing(client.fetchTopRatedHorrorPage(page));
            } catch (ItchTransientException e) {
                log.warn("itch.io top-rated page {} failed, stopping discovery: {}", page, e.getMessage());
                break;
            }
            if (listings.isEmpty()) {
                break;
            }
            for (ItchListing listing : listings) {
                if (writer.recordListing(listing, config.minRatings())) {
                    discovered++;
                }
            }
        }
        return discovered;
    }

    private Outcome fetchPages(Long jobId) {
        IngestionProperties.Itch config = properties.itch();
        if (config.maxPerRun() == 0) {
            return new Outcome(0, 0);
        }
        Instant now = clock.instant();
        List<ItchGameSeed> targets = seedRepository.findFetchTargets(now.minus(config.refreshAfter()),
                now.minus(config.failedRetryAfter()), config.maxFailCount(), Limit.of(config.maxPerRun()));
        int processed = 0;
        int failed = 0;
        for (ItchGameSeed seed : targets) {
            jobRecorder.progress(jobId, processed, failed);
            try {
                String html = client.fetchGamePage(seed.getUrl());
                if (html == null) {
                    writer.markNotFound(seed.getItchId());
                    processed++;
                    continue;
                }
                ParsedItchGame parsed = parser.parseGame(html);
                if (parsed.itchId() != seed.getItchId()) {
                    // 주소가 다른 게임으로 바뀌었다. 목록에서 새 주소를 다시 보면 고쳐진다
                    log.warn("itch.io page {} is now game {}, expected {}", seed.getUrl(), parsed.itchId(),
                            seed.getItchId());
                    writer.markFailed(seed.getItchId());
                    failed++;
                    continue;
                }
                writer.apply(seed.getUrl(), parsed, seed.getDiscoveredBy());
                processed++;
            } catch (ItchTransientException | ItchPageParseException e) {
                log.warn("itch.io game {} failed: {}", seed.getItchId(), e.getMessage());
                writer.markFailed(seed.getItchId());
                failed++;
            }
        }
        return new Outcome(processed, failed);
    }

    private record Outcome(int processed, int failed) {
    }
}
