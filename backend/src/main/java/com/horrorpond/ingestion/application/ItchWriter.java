package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.ItchGameData;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.repository.GameMetricDailyRepository;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.util.SlugGenerator;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.ItchDiscoveredBy;
import com.horrorpond.ingestion.domain.ItchGameSeed;
import com.horrorpond.ingestion.repository.ItchGameSeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * itch.io 결과 1건을 1트랜잭션으로 반영한다. catalog 서비스가 아니라 Repository만 사용한다.
 * 평가 수를 받을 때마다 그날의 기록(game_metric_daily, store=ITCH)도 남긴다.
 */
@Component
@RequiredArgsConstructor
public class ItchWriter {

    private final ItchGameSeedRepository seedRepository;
    private final GameRepository gameRepository;
    private final GameMetricDailyRepository metricRepository;
    private final DeveloperResolver developerResolver;
    private final Clock clock;

    /**
     * 목록에서 본 게임. 이미 아는 게임이면 주소와 평가 수를 갱신하고, 처음 보는 게임은 평가 수가 기준 이상일 때만 받는다.
     *
     * @return 새로 발견했으면 true
     */
    @Transactional
    public boolean recordListing(ItchListing listing, int minRatings) {
        Instant now = clock.instant();
        ItchGameSeed seed = seedRepository.findById(listing.itchId()).orElse(null);
        boolean created = false;
        if (seed == null) {
            if (listing.ratingCount() == null || listing.ratingCount() < minRatings) {
                return false;
            }
            seed = seedRepository.save(ItchGameSeed.discovered(listing.itchId(), listing.url(),
                    ItchDiscoveredBy.TOP_RATED, now));
            created = true;
        }
        seed.seen(listing.url(), listing.ratingCount());
        if (listing.ratingCount() != null) {
            gameRepository.findBySourceAndExternalId(GameSource.ITCH, String.valueOf(listing.itchId()))
                    .ifPresent(game -> {
                        game.updateItchRatingCount(listing.ratingCount());
                        recordMetric(game, listing.ratingCount(), now);
                    });
        }
        return created;
    }

    /**
     * 게임 페이지 내용을 반영한다. seed가 없으면(관리자 추가) 만든다.
     * 자동 발견한 게임이 Steam에 같은 이름으로 이미 있으면 같은 게임이 두 번 보이지 않게 만들지 않는다
     * (관리자가 주소로 추가하면 그대로 만든다).
     *
     * @return 게임 id. Steam에 있어 만들지 않았으면 null
     */
    @Transactional
    public Long apply(String url, ParsedItchGame parsed, ItchDiscoveredBy discoveredBy) {
        Instant now = clock.instant();
        ItchGameSeed seed = seedRepository.findById(parsed.itchId())
                .orElseGet(() -> seedRepository.save(ItchGameSeed.discovered(parsed.itchId(), url, discoveredBy, now)));
        if (discoveredBy == ItchDiscoveredBy.MANUAL) {
            seed.requestManually(url);
        } else {
            seed.seen(url, parsed.ratingCount());
        }

        Game existing = gameRepository.findBySourceAndExternalId(GameSource.ITCH, String.valueOf(parsed.itchId()))
                .orElse(null);
        if (existing == null && discoveredBy != ItchDiscoveredBy.MANUAL
                && gameRepository.existsBySourceAndTitle(GameSource.STEAM, parsed.title())) {
            seed.markFetched(FetchStatus.OK, parsed.ratingCount(), now);
            return null;
        }
        Game game = existing != null ? existing
                : gameRepository.save(Game.candidateFromItch(parsed.itchId(), parsed.title(),
                        NormalizeItemProcessor.uniqueSlug(SlugGenerator.slugify(parsed.title()),
                                gameRepository::existsBySlug)));
        List<Developer> developers = parsed.authors().stream().map(developerResolver::findOrCreate).toList();
        game.applyItchData(new ItchGameData(url, parsed.title(), parsed.description(), parsed.coverUrl(),
                parsed.releaseDate(), ItchPageParser.koreanDateText(parsed.releaseDate()),
                ItchPageParser.englishDateText(parsed.releaseDate()), parsed.ratingCount(), parsed.tags(),
                parsed.languages(), parsed.screenshots(), developers));
        if (parsed.ratingCount() != null) {
            recordMetric(game, parsed.ratingCount(), now);
        }
        seed.markFetched(FetchStatus.OK, parsed.ratingCount(), now);
        return game.getId();
    }

    /** 삭제·비공개된 게임. 이미 받은 게임은 그대로 두고(관리자가 숨길 수 있다) 갱신 주기마다 다시 확인한다 */
    @Transactional
    public void markNotFound(long itchId) {
        seedRepository.findById(itchId).ifPresent(seed -> seed.markFetched(FetchStatus.NOT_FOUND, null, clock.instant()));
    }

    @Transactional
    public void markFailed(long itchId) {
        seedRepository.findById(itchId).ifPresent(seed -> seed.markFailed(clock.instant()));
    }

    private void recordMetric(Game game, int ratingCount, Instant now) {
        metricRepository.upsert(game.getId(), Store.ITCH.name(), LocalDate.ofInstant(now, GameMetricDaily.ZONE),
                ratingCount, now);
    }
}
