package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.repository.GameMetricDailyRepository;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.util.SlugGenerator;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import com.horrorpond.ingestion.repository.SteamRawSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 스냅샷 1건을 1트랜잭션으로 Game에 반영한다. catalog 서비스가 아니라 Repository만 사용한다.
 * 리뷰 수가 있으면 스냅샷을 받은 날의 리뷰 수 기록(game_metric_daily)도 남긴다.
 */
@Component
@RequiredArgsConstructor
public class NormalizeItemProcessor {

    private final SteamRawSnapshotRepository snapshotRepository;
    private final SteamAppSeedRepository seedRepository;
    private final GameRepository gameRepository;
    private final DeveloperResolver developerResolver;
    private final SteamAppDetailsParser parser;
    private final GameMetricDailyRepository metricRepository;

    @Transactional
    public void process(int appid) {
        SteamRawSnapshot snapshot = snapshotRepository.findById(appid).orElseThrow();
        ParsedSteamApp parsed = parser.parse(snapshot.getPayload());
        if (parsed.isGame()) {
            SteamGameData data = parsed.data();
            // 주소는 영어 이름이 있으면 그걸로 만든다 (한국어 이름은 slug로 바꿀 수 없어 appid만 남는다)
            String slugSource = parsed.english() != null && parsed.english().title() != null
                    ? parsed.english().title() : data.title();
            Game game = gameRepository.findBySourceAndExternalId(GameSource.STEAM, String.valueOf(appid))
                    .orElseGet(() -> gameRepository.save(Game.candidateFromSteam(appid, data.title(),
                            uniqueSlug(SlugGenerator.forSteamCandidate(slugSource, appid),
                                    gameRepository::existsBySlug))));
            boolean freshReviews = hasFreshReviewCount(game, data, snapshot);
            // 스냅샷이 METRICS가 받은 리뷰 수보다 오래됐거나(재정리) 리뷰 수를 못 받았으면 지금 값을 지킨다
            SteamGameData applied = freshReviews ? data : data.withReviewCount(game.getReviewCount());
            game.applySteamData(applied.withDevelopers(credits(parsed)));
            if (parsed.english() != null) {
                game.applyEnglishText(parsed.english().title(), parsed.english().shortDescription(),
                        parsed.english().releaseDateText());
            }
            seedRepository.findById(appid)
                    .map(SteamAppSeed::getSpyTags)
                    .ifPresent(game::applySteamTags);
            game.applySteamLanguages(parsed.languages().languages(), parsed.languages().audioLanguages());
            game.applyPrice(parsed.price());
            if (freshReviews) {
                Instant fetchedAt = snapshot.getFetchedAt();
                metricRepository.upsert(game.getId(), Store.STEAM.name(),
                        LocalDate.ofInstant(fetchedAt, GameMetricDaily.ZONE), data.reviewCount(), fetchedAt);
            }
        }
        snapshot.markNormalized();
    }

    /**
     * 스냅샷의 리뷰 수를 반영해도 되는지. 리뷰 수가 있고, 그 뒤에 받은 기록(METRICS)이 없어야 한다.
     * 정리를 다시 돌릴 때(예: 컬럼 추가 후 전체 재정리) 며칠 지난 스냅샷이 최신 리뷰 수를 되돌리지 않게 한다.
     */
    private boolean hasFreshReviewCount(Game game, SteamGameData data, SteamRawSnapshot snapshot) {
        if (data.reviewCount() == null) {
            return false;
        }
        if (game.getId() == null) {
            return true;
        }
        Instant latest = metricRepository.findLatestCapturedAt(game.getId(), Store.STEAM.name());
        return latest == null || !latest.isAfter(snapshot.getFetchedAt());
    }

    private List<SteamGameData.Credit> credits(ParsedSteamApp parsed) {
        List<SteamGameData.Credit> credits = new ArrayList<>();
        parsed.developerNames().forEach(name ->
                credits.add(new SteamGameData.Credit(findOrCreateDeveloper(name), DeveloperRole.DEVELOPER)));
        parsed.publisherNames().forEach(name ->
                credits.add(new SteamGameData.Credit(findOrCreateDeveloper(name), DeveloperRole.PUBLISHER)));
        return credits;
    }

    private Developer findOrCreateDeveloper(String name) {
        return developerResolver.findOrCreate(name);
    }

    /**
     * 이미 쓰인 slug면 "-2", "-3"... 접미사를 붙인다.
     */
    static String uniqueSlug(String base, Predicate<String> exists) {
        String candidate = base;
        for (int suffix = 2; exists.test(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
