package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.curation.application.ExposurePolicy;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.ItchDiscoveredBy;
import com.horrorpond.ingestion.domain.ItchGameSeed;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import com.horrorpond.ingestion.repository.ItchGameSeedRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.SteamMockServer;
import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ADMIN_API_KEY=" + ItchIngestionIntegrationTest.KEY,
        "ingestion.itch.max-pages=3",
        "ingestion.itch.max-per-run=10",
        "ingestion.itch.min-ratings=500"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ItchIngestionIntegrationTest {

    static final String KEY = "test-admin-key";
    private static final MockRestServiceServer SERVER = SteamMockServer.SERVER;

    @TestBean(name = "itchRestClient", methodName = "com.horrorpond.support.SteamMockServer#itchRestClient")
    RestClient itchRestClient;

    @MockitoBean
    Sleeper sleeper;

    @Autowired
    ItchIngestionService itchService;

    @Autowired
    ItchGameSeedRepository seedRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    ExposurePolicy exposurePolicy;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    MockMvc mvc;

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
    void collectsOnlyPopularGamesFromTopRatedList() {
        expectListing(1,
                ItchFixtures.listingCell(10, "https://dev.itch.io/night-shift", "Night Shift", 900),
                ItchFixtures.listingCell(11, "https://jam.itch.io/tiny-jam", "Tiny Jam", 120),   // 평가 적음 → 제외
                ItchFixtures.listingCell(12, "https://new.itch.io/no-ratings", "No Ratings", null));
        expectListing(2, ItchFixtures.listingCell(13, "https://big.itch.io/classic", "Classic", 5000));
        expectListing(3);  // 빈 페이지면 거기서 멈춘다
        expectPage("https://dev.itch.io/night-shift", ItchFixtures.gamePage(10, "Night Shift", "Clock in.", 900));
        expectPage("https://big.itch.io/classic", ItchFixtures.gamePage(13, "Classic", "Old but gold.", 5000));

        assertJob(itchService.run(TriggerType.MANUAL), 2, 0);
        SERVER.verify();

        assertThat(seedRepository.findAll()).extracting(ItchGameSeed::getItchId).containsExactlyInAnyOrder(10L, 13L);
        inGame(10, game -> {
            assertThat(game.getSource()).isEqualTo(GameSource.ITCH);
            assertThat(game.getStatus()).isEqualTo(GameStatus.CANDIDATE);
            assertThat(game.getSlug()).isEqualTo("night-shift");
            assertThat(game.getTitle()).isEqualTo("Night Shift");
            assertThat(game.getShortDescription()).isEqualTo("Clock in.");
            assertThat(game.getReviewCount()).isEqualTo(900);
            assertThat(game.getReleaseDate()).isEqualTo(LocalDate.of(2024, 3, 3));
            assertThat(game.getReleaseDateText()).isEqualTo("2024년 3월 3일");
            assertThat(game.getReleaseDateTextEn()).isEqualTo("Mar 3, 2024");
            assertThat(game.getTags()).containsExactly("Adventure", "Horror", "Psychological Horror");
            assertThat(game.getLanguages()).containsExactly("en", "ko", "pt-BR");
            assertThat(game.getMedia()).hasSize(2);
            assertThat(game.getDevelopers()).extracting(d -> d.getDeveloper().getName()).containsExactly("Dev Studio");
            assertThat(game.getStoreLinks()).singleElement().satisfies(link -> {
                assertThat(link.getStore()).isEqualTo(Store.ITCH);
                assertThat(link.getUrl()).isEqualTo("https://dev.itch.io/night-shift");
            });
            // 평가 수가 많아 자동 노출된다
            assertThat(exposurePolicy.isVisible(game)).isTrue();
        });
        assertThat(ratingRecordedToday(10)).isEqualTo(900);
    }

    @Test
    void knownGamesGetRatingCountsFromTheListWithoutRefetchingPages() {
        expectListing(1, ItchFixtures.listingCell(10, "https://dev.itch.io/night-shift", "Night Shift", 900));
        expectListing(2);
        expectPage("https://dev.itch.io/night-shift", ItchFixtures.gamePage(10, "Night Shift", "Clock in.", 900));
        itchService.run(TriggerType.MANUAL);
        SERVER.verify();
        SERVER.reset();

        // 다음 실행: 평가 수가 늘었고, 기준 아래로 떨어진 적 없는 게임은 페이지를 다시 받지 않는다 (7일 주기)
        expectListing(1, ItchFixtures.listingCell(10, "https://dev.itch.io/night-shift-renamed", "Night Shift", 950));
        expectListing(2);

        assertJob(itchService.run(TriggerType.MANUAL), 0, 0);
        SERVER.verify();

        inGame(10, game -> assertThat(game.getReviewCount()).isEqualTo(950));
        assertThat(ratingRecordedToday(10)).isEqualTo(950);
        assertThat(seedRepository.findById(10L).orElseThrow().getUrl()).isEqualTo("https://dev.itch.io/night-shift-renamed");
    }

    @Test
    void popularItchGamesAlreadyOnSteamAreNotDuplicated() {
        tx.executeWithoutResult(status -> {
            Game steam = gameRepository.save(Game.candidateFromSteam(100, "고전 명작", "classic-100"));
            steam.applyEnglishText("Classic", null, null);
        });
        expectListing(1, ItchFixtures.listingCell(13, "https://big.itch.io/classic", "Classic", 5000));
        expectListing(2);
        expectPage("https://big.itch.io/classic", ItchFixtures.gamePage(13, "CLASSIC", "Old but gold.", 5000));

        assertJob(itchService.run(TriggerType.MANUAL), 1, 0);
        SERVER.verify();

        assertThat(gameRepository.findBySourceAndExternalId(GameSource.ITCH, "13")).isEmpty();
        // 페이지는 받았으니 갱신 주기 전까지 다시 받지 않는다
        assertThat(seedRepository.findById(13L).orElseThrow().getFetchStatus()).isEqualTo(FetchStatus.OK);
    }

    @Test
    void deletedGamesAndBrokenPagesAreRecordedOnSeeds() {
        expectListing(1,
                ItchFixtures.listingCell(20, "https://gone.itch.io/deleted", "Deleted", 800),
                ItchFixtures.listingCell(21, "https://odd.itch.io/broken", "Broken", 800));
        expectListing(2);
        SERVER.expect(once(), requestTo("https://gone.itch.io/deleted")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectPage("https://odd.itch.io/broken", "<html>not a game</html>");

        assertJob(itchService.run(TriggerType.MANUAL), 1, 1);
        SERVER.verify();

        assertThat(seedRepository.findById(20L).orElseThrow().getFetchStatus()).isEqualTo(FetchStatus.NOT_FOUND);
        ItchGameSeed broken = seedRepository.findById(21L).orElseThrow();
        assertThat(broken.getFetchStatus()).isEqualTo(FetchStatus.FAILED);
        assertThat(broken.getFailCount()).isEqualTo(1);
        assertThat(gameRepository.count()).isZero();
    }

    @Test
    void adminAddsAnyItchGameByUrl() throws Exception {
        // 평가가 적은 게임도 관리자가 고르면 들어온다 (공개는 관리 화면에서 고정 노출)
        expectPage("https://dev.itch.io/small-game", ItchFixtures.gamePage(30, "Small Game", "A short one.", 3)
                .replaceAll("<tr><td>Published</td>.*?</tr>", ""));

        String body = mvc.perform(post("/api/admin/ingestion/itch")
                        .header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://Dev.itch.io/Small-Game/\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNumber())
                .andReturn().getResponse().getContentAsString();
        SERVER.verify();

        assertThat(body).contains(String.valueOf(gameRepository.findBySourceAndExternalId(GameSource.ITCH, "30")
                .orElseThrow().getId()));
        assertThat(seedRepository.findById(30L).orElseThrow().getDiscoveredBy()).isEqualTo(ItchDiscoveredBy.MANUAL);
        inGame(30, game -> assertThat(exposurePolicy.isVisible(game)).isFalse());
    }

    @Test
    void adminAddRejectsNonItchUrlsAndMissingGames() throws Exception {
        mvc.perform(post("/api/admin/ingestion/itch")
                        .header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/game\"}"))
                .andExpect(status().isBadRequest());

        SERVER.expect(once(), requestTo("https://dev.itch.io/missing")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        mvc.perform(post("/api/admin/ingestion/itch")
                        .header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://dev.itch.io/missing\"}"))
                .andExpect(status().isNotFound());
        SERVER.verify();
    }

    private void expectListing(int page, String... cells) {
        SERVER.expect(once(), requestTo(SteamMockServer.itchTopRatedUrl(page)))
                .andRespond(withSuccess(ItchFixtures.listingJson(cells), MediaType.APPLICATION_JSON));
    }

    private void expectPage(String url, String html) {
        SERVER.expect(once(), requestTo(url)).andRespond(withSuccess(html, MediaType.TEXT_HTML));
    }

    private void inGame(long itchId, Consumer<Game> assertions) {
        tx.executeWithoutResult(status -> assertions.accept(
                gameRepository.findBySourceAndExternalId(GameSource.ITCH, String.valueOf(itchId)).orElseThrow()));
    }

    private Integer ratingRecordedToday(long itchId) {
        return jdbc.queryForObject("""
                SELECT m.review_count FROM game_metric_daily m JOIN game g ON g.id = m.game_id
                WHERE g.source = 'ITCH' AND g.external_id = ? AND m.store = 'ITCH' AND m.captured_on = ?""",
                Integer.class, String.valueOf(itchId), today);
    }

    private void assertJob(Long jobId, int processed, int failed) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(JobType.ITCH);
        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getProcessedCount()).isEqualTo(processed);
        assertThat(job.getFailedCount()).isEqualTo(failed);
    }
}
