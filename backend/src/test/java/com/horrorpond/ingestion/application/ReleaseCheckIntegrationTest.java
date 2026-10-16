package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import com.horrorpond.ingestion.repository.SteamRawSnapshotRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.Fixtures;
import com.horrorpond.support.SteamMockServer;
import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 매시간 출시 확인. 알림 메일은 IngestionPipeline만 보내므로 이 서비스에는 알림 경로가 없다.
 * 테스트 컨텍스트(DB 연결 수)를 늘리지 않도록 다른 수집 통합 테스트와 같은 빈 교체만 쓴다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReleaseCheckIntegrationTest {

    private static final MockRestServiceServer SERVER = SteamMockServer.SERVER;
    private static final int PHASMO = 739630;

    @TestBean(name = "steamStoreRestClient", methodName = "com.horrorpond.support.SteamMockServer#storeRestClient")
    RestClient steamStoreRestClient;

    @TestBean(name = "steamSpyRestClient", methodName = "com.horrorpond.support.SteamMockServer#spyRestClient")
    RestClient steamSpyRestClient;

    @MockitoBean
    Sleeper sleeper;

    @Autowired
    ReleaseCheckService releaseCheckService;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    SteamAppSeedRepository seedRepository;

    @Autowired
    SteamRawSnapshotRepository snapshotRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbc);
        SERVER.reset();
        today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"));
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void targetsAreComingSoonGamesReleasingYesterdayToTomorrow() {
        Instant twoHoursAgo = clock.instant().minus(Duration.ofHours(2));
        steamGame(1, true, today.minusDays(1), twoHoursAgo, false);    // 어제인데 아직 출시 예정 → 대상
        steamGame(2, true, today, twoHoursAgo, false);                 // 오늘 → 대상
        steamGame(3, true, today.plusDays(1), twoHoursAgo, false);     // 내일 → 대상
        steamGame(4, true, today.plusDays(2), twoHoursAgo, false);     // 모레 → 제외
        steamGame(5, true, today.minusDays(2), twoHoursAgo, false);    // 그저께 → 제외 (하루 1회 수집이 맡는다)
        steamGame(6, false, today, twoHoursAgo, false);                // 이미 출시됨 → 제외
        steamGame(7, true, today, twoHoursAgo, true);                  // 숨김 → 제외
        steamGame(8, true, today, clock.instant().minus(Duration.ofMinutes(10)), false); // 방금 받음 → 제외

        assertThat(releaseCheckService.selectTargets())
                .extracting(SteamAppSeed::getAppid)
                .containsExactly(1, 2, 3);
    }

    @Test
    void releasedGameFlipsToReleasedThroughSnapshotAndNormalize() {
        steamGame(PHASMO, true, today, clock.instant().minus(Duration.ofHours(2)), false);
        expectAppDetails(PHASMO, released(PHASMO, today));
        expectEnglish(PHASMO);

        Long jobId = releaseCheckService.run(TriggerType.SCHEDULED);
        SERVER.verify();

        assertJob(jobId, JobStatus.SUCCEEDED, 1, 0);
        Game game = gameRepository.findBySourceAndExternalId(GameSource.STEAM, String.valueOf(PHASMO)).orElseThrow();
        assertThat(game.isComingSoon()).isFalse();
        assertThat(game.getReleaseDate()).isEqualTo(today);
        assertThat(game.getPriceAmount()).as("출시되면 정가가 들어온다").isEqualTo(2150000);
        assertThat(game.getReviewCount()).isEqualTo(684837);
        assertThat(snapshotRepository.findById(PHASMO).orElseThrow().needsNormalize()).isFalse();
        SteamAppSeed seed = seedRepository.findById(PHASMO).orElseThrow();
        assertThat(seed.getFetchStatus()).isEqualTo(FetchStatus.OK);
        assertThat(seed.getLastFetchedAt()).isAfter(clock.instant().minus(Duration.ofMinutes(5)));
        // 방금 받았으므로 다음 실행 대상이 아니다
        assertThat(releaseCheckService.selectTargets()).isEmpty();
    }

    @Test
    void noTargetsCreatesNoJob() {
        steamGame(1, true, today.plusDays(5), clock.instant().minus(Duration.ofHours(2)), false);

        assertThat(releaseCheckService.run(TriggerType.SCHEDULED)).isNull();
        SERVER.verify();
        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void failedItemDoesNotStopTheRunAndKeepsSeedState() {
        Instant twoHoursAgo = clock.instant().minus(Duration.ofHours(2));
        steamGame(1, true, today.minusDays(1), twoHoursAgo, false);
        steamGame(PHASMO, true, today, twoHoursAgo, false);
        SERVER.expect(ExpectedCount.times(3), requestTo(SteamMockServer.appDetailsUrl(1)))
                .andRespond(withServerError());
        expectAppDetails(PHASMO, released(PHASMO, today));
        expectEnglish(PHASMO);

        Long jobId = releaseCheckService.run(TriggerType.SCHEDULED);
        SERVER.verify();

        assertJob(jobId, JobStatus.SUCCEEDED, 1, 1);
        SteamAppSeed failed = seedRepository.findById(1).orElseThrow();
        assertThat(failed.getFetchStatus()).as("다음 시간에 다시 받도록 그대로 둔다").isEqualTo(FetchStatus.OK);
        assertThat(failed.getFailCount()).isZero();
        assertThat(gameRepository.findBySourceAndExternalId(GameSource.STEAM, String.valueOf(PHASMO))
                .orElseThrow().isComingSoon()).isFalse();
    }

    @Test
    void repeatedRateLimitsAbortTheRunWithoutLongWaits() {
        Instant twoHoursAgo = clock.instant().minus(Duration.ofHours(2));
        steamGame(PHASMO, true, today, twoHoursAgo, false);
        steamGame(2, true, today.plusDays(1), twoHoursAgo, false);
        SERVER.expect(ExpectedCount.times(2), requestTo(SteamMockServer.appDetailsUrl(PHASMO)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Long jobId = releaseCheckService.run(TriggerType.SCHEDULED);
        SERVER.verify();

        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(JobType.RELEASE_CHECK);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Rate limited 2 times in a row");
        verify(sleeper, times(1)).sleep(eq(Duration.ofSeconds(60)));
    }

    private void steamGame(int appid, boolean comingSoon, LocalDate releaseDate, Instant fetchedAt, boolean hidden) {
        Game game = Game.candidateFromSteam(appid, "Game " + appid, "game-" + appid);
        game.applySteamData(new SteamGameData("Game " + appid, null, null, releaseDate, releaseDate.toString(),
                comingSoon, false, null, false, List.of(), List.of()));
        if (hidden) {
            game.hide();
        }
        gameRepository.save(game);
        SteamAppSeed seed = SteamAppSeed.discovered(appid, DiscoveredBy.STEAM_SEARCH, fetchedAt);
        seed.markFetched(FetchStatus.OK, fetchedAt);
        seedRepository.save(seed);
    }

    /** Phasmophobia appdetails를 오늘 출시된 것으로 바꾼다 */
    private static String released(int appid, LocalDate date) {
        String body = Fixtures.appDetails(appid);
        String original = "\"release_date\":{\"coming_soon\":false,\"date\":\"2020년 9월 18일\"}";
        assertThat(body).contains(original);
        return body.replace(original, "\"release_date\":{\"coming_soon\":false,\"date\":\""
                + date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일\"}");
    }

    private void expectAppDetails(int appid, String body) {
        SERVER.expect(once(), requestTo(SteamMockServer.appDetailsUrl(appid)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectEnglish(int appid) {
        SERVER.expect(once(), requestTo(SteamMockServer.appDetailsEnglishUrl(appid)))
                .andRespond(withSuccess("{\"" + appid + "\":{\"success\":true,\"data\":{\"name\":\"Phasmophobia\","
                        + "\"short_description\":\"English\",\"release_date\":{\"coming_soon\":false,"
                        + "\"date\":\"Oct 16, 2026\"}}}}", MediaType.APPLICATION_JSON));
    }

    private void assertJob(Long jobId, JobStatus status, int processed, int failed) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(JobType.RELEASE_CHECK);
        assertThat(job.getTriggerType()).isEqualTo(TriggerType.SCHEDULED);
        assertThat(job.getStatus()).as("error=%s", job.getErrorMessage()).isEqualTo(status);
        assertThat(job.getProcessedCount()).isEqualTo(processed);
        assertThat(job.getFailedCount()).isEqualTo(failed);
    }
}
