package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.repository.GameMetricDailyRepository.SteamMetricTarget;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import com.horrorpond.support.DatabaseCleaner;
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

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReviewMetricsIntegrationTest {

    private static final MockRestServiceServer SERVER = SteamMockServer.SERVER;

    @TestBean(name = "steamStoreRestClient", methodName = "com.horrorpond.support.SteamMockServer#storeRestClient")
    RestClient steamStoreRestClient;

    @TestBean(name = "steamSpyRestClient", methodName = "com.horrorpond.support.SteamMockServer#spyRestClient")
    RestClient steamSpyRestClient;

    @MockitoBean
    Sleeper sleeper;

    @Autowired
    ReviewMetricsService metricsService;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(GameMetricDaily.ZONE);

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbc);
        SERVER.reset();
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void recordsRecentAndPopularGamesAndUpdatesReviewCount() {
        steamGame(1, today.minusDays(3), false, 2, false);        // 최근 출시
        steamGame(2, today.minusYears(3), false, 5000, false);    // 오래됐지만 리뷰 많음
        steamGame(3, today.minusYears(3), false, 3, false);       // 오래됐고 리뷰 적음 → 제외
        steamGame(4, today.plusDays(5), true, 0, false);          // 출시 전 → 제외
        steamGame(5, today.minusDays(1), false, 100, true);       // 성인 → 제외
        steamGame(6, today.minusDays(2), false, 50, false, Game::hide); // 숨김 → 제외
        expectReviews(1, 12);
        expectReviews(2, 5100);

        assertJob(metricsService.run(TriggerType.MANUAL), JobStatus.SUCCEEDED, 2, 0);
        SERVER.verify();

        assertThat(reviewCount(1)).isEqualTo(12);
        assertThat(reviewCount(2)).isEqualTo(5100);
        assertThat(jdbc.queryForList("""
                SELECT g.external_id FROM game_metric_daily m JOIN game g ON g.id = m.game_id
                WHERE m.captured_on = ? AND m.store = 'STEAM' ORDER BY g.external_id""", String.class, today))
                .containsExactly("1", "2");

        // 같은 날 다시 돌리면 이미 기록한 게임은 건너뛴다
        SERVER.reset();
        assertJob(metricsService.run(TriggerType.MANUAL), JobStatus.SUCCEEDED, 0, 0);
        SERVER.verify();
    }

    @Test
    void recentReleasesComeFirstThenLeastRecentlyRecorded() {
        steamGame(1, today.minusYears(2), false, 900, false);
        steamGame(2, today.minusYears(2), false, 800, false);
        steamGame(3, today.minusDays(5), false, 1, false);
        // 2번은 어제 기록했고 1번은 기록이 없다 → 최근 출시 3번, 기록 없는 1번, 어제 기록한 2번 순서
        jdbc.update("""
                INSERT INTO game_metric_daily (game_id, store, captured_on, review_count)
                SELECT id, 'STEAM', ?, 790 FROM game WHERE external_id = '2'""", today.minusDays(1));

        assertThat(metricsService.selectTargets())
                .extracting(SteamMetricTarget::getAppid)
                .containsExactly("3", "1", "2");
    }

    @Test
    void abortsAfterConsecutiveRateLimits() {
        steamGame(1, today.minusDays(1), false, 0, false);
        SERVER.expect(ExpectedCount.times(3), requestTo(reviewsUrl(1)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Long jobId = metricsService.run(TriggerType.MANUAL);
        SERVER.verify();

        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Rate limited 3 times in a row");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM game_metric_daily", Integer.class)).isZero();
    }

    private void steamGame(int appid, LocalDate releaseDate, boolean comingSoon, int reviewCount, boolean adult) {
        steamGame(appid, releaseDate, comingSoon, reviewCount, adult, game -> { });
    }

    private void steamGame(int appid, LocalDate releaseDate, boolean comingSoon, int reviewCount, boolean adult,
                           Consumer<Game> customize) {
        Game game = Game.candidateFromSteam(appid, "Game " + appid, "game-" + appid);
        game.applySteamData(new SteamGameData("Game " + appid, null, null, releaseDate, null, comingSoon, false,
                reviewCount, adult, List.of(), List.of()));
        customize.accept(game);
        gameRepository.save(game);
    }

    private Integer reviewCount(int appid) {
        return jdbc.queryForObject("SELECT review_count FROM game WHERE external_id = ?", Integer.class,
                String.valueOf(appid));
    }

    private void expectReviews(int appid, int total) {
        SERVER.expect(once(), requestTo(reviewsUrl(appid))).andRespond(withSuccess(
                "{\"success\":1,\"query_summary\":{\"num_reviews\":0,\"total_reviews\":" + total + "}}",
                MediaType.APPLICATION_JSON));
    }

    private static String reviewsUrl(int appid) {
        return SteamMockServer.STORE_BASE + "/appreviews/" + appid
                + "?json=1&language=all&purchase_type=all&num_per_page=0";
    }

    private void assertJob(Long jobId, JobStatus status, int processed, int failed) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(JobType.METRICS);
        assertThat(job.getStatus()).isEqualTo(status);
        assertThat(job.getProcessedCount()).isEqualTo(processed);
        assertThat(job.getFailedCount()).isEqualTo(failed);
    }
}
