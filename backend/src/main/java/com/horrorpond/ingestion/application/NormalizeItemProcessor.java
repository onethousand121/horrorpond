package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.repository.DeveloperRepository;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 스냅샷 1건을 1트랜잭션으로 Game에 반영한다. catalog 서비스가 아니라 Repository만 사용한다.
 * 리뷰 수가 있으면 그날의 리뷰 수 기록(game_metric_daily)도 남긴다.
 */
@Component
@RequiredArgsConstructor
public class NormalizeItemProcessor {

    private static final String DEVELOPER_SLUG_FALLBACK = "developer";

    private final SteamRawSnapshotRepository snapshotRepository;
    private final SteamAppSeedRepository seedRepository;
    private final GameRepository gameRepository;
    private final DeveloperRepository developerRepository;
    private final SteamAppDetailsParser parser;
    private final GameMetricDailyRepository metricRepository;
    private final Clock clock;

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
            game.applySteamData(data.withDevelopers(credits(parsed)));
            if (parsed.english() != null) {
                game.applyEnglishText(parsed.english().title(), parsed.english().shortDescription(),
                        parsed.english().releaseDateText());
            }
            seedRepository.findById(appid)
                    .map(SteamAppSeed::getSpyTags)
                    .ifPresent(game::applySteamTags);
            if (data.reviewCount() != null) {
                metricRepository.upsert(game.getId(), Store.STEAM.name(),
                        LocalDate.ofInstant(clock.instant(), GameMetricDaily.ZONE), data.reviewCount(), clock.instant());
            }
        }
        snapshot.markNormalized();
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
        return developerRepository.findByName(name)
                .orElseGet(() -> developerRepository.save(Developer.create(name,
                        uniqueSlug(SlugGenerator.slugify(name, DEVELOPER_SLUG_FALLBACK),
                                developerRepository::existsBySlug))));
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
