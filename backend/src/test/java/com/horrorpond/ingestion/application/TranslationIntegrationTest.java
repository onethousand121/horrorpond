package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.ingestion.client.SteamClientConfig;
import com.horrorpond.ingestion.client.TranslationProperties;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "DEEPL_API_KEY=test-key:fx",
        "translation.monthly-reserve=0",
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TranslationIntegrationTest {

    /** 실제 설정(SteamClientConfig)의 주소·인증 헤더에 가짜 서버를 붙인다 */
    private static final RestClient.Builder BUILDER = SteamClientConfig.configureDeepL(RestClient.builder(),
            new TranslationProperties(null, 150000, 0, 50, 30), "test-key:fx");
    private static final MockRestServiceServer SERVER = MockRestServiceServer.bindTo(BUILDER).build();

    @TestBean(name = "deepLRestClient")
    RestClient deepLRestClient;

    static RestClient deepLRestClient() {
        return BUILDER.build();
    }

    @Autowired
    TranslationService translationService;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

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
    void translatesEnglishOnlyDescriptionsMostPopularFirst() throws Exception {
        steamGame(1, "Escape the asylum.", 500);
        steamGame(2, "A cursed tape.", 9000);
        steamGame(3, "이미 한국어 소개", 100);
        steamGame(4, "Hidden game.", 100, Game::hide);
        steamGame(5, "Adult game.", 100, true);
        expectUsage(0, 500000);
        SERVER.expect(once(), requestTo("https://api-free.deepl.com/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "DeepL-Auth-Key test-key:fx"))
                .andExpect(content().json("""
                        {"text":["A cursed tape.","Escape the asylum."],"target_lang":"KO"}"""))
                .andRespond(withSuccess("""
                        {"translations":[{"detected_source_language":"EN","text":"저주받은 테이프."},
                                         {"detected_source_language":"EN","text":"정신병원을 탈출하라."}]}""",
                        MediaType.APPLICATION_JSON));

        assertJob(translationService.run(TriggerType.MANUAL), JobStatus.SUCCEEDED, 2, 0);
        SERVER.verify();

        assertThat(koAuto(2)).isEqualTo("저주받은 테이프.");
        assertThat(koAuto(1)).isEqualTo("정신병원을 탈출하라.");
        assertThat(koAuto(3)).isNull();
        assertThat(koAuto(4)).isNull();

        mvc.perform(get("/api/games/game-2").param("lang", "ko"))
                .andExpect(jsonPath("$.shortDescription").value("저주받은 테이프."))
                .andExpect(jsonPath("$.shortDescriptionTranslated").value(true));
        mvc.perform(get("/api/games/game-2").param("lang", "en"))
                .andExpect(jsonPath("$.shortDescription").value("A cursed tape."))
                .andExpect(jsonPath("$.shortDescriptionTranslated").value(false));
        mvc.perform(get("/api/games").param("sort", "POPULAR").param("lang", "ko"))
                .andExpect(jsonPath("$.content[0].shortDescription").value("저주받은 테이프."))
                .andExpect(jsonPath("$.content[0].shortDescriptionTranslated").value(true));
    }

    @Test
    void stopsAtRemainingMonthlyQuota() {
        steamGame(1, "Escape the asylum.", 500);   // 18자
        steamGame(2, "A cursed tape.", 9000);      // 14자
        expectUsage(499980, 500000);               // 20자 남음 → 리뷰 많은 2번만
        SERVER.expect(once(), requestTo("https://api-free.deepl.com/v2/translate"))
                .andExpect(content().json("""
                        {"text":["A cursed tape."]}"""))
                .andRespond(withSuccess("""
                        {"translations":[{"text":"저주받은 테이프."}]}""", MediaType.APPLICATION_JSON));

        assertJob(translationService.run(TriggerType.MANUAL), JobStatus.SUCCEEDED, 1, 0);
        SERVER.verify();
        assertThat(koAuto(1)).isNull();
    }

    @Test
    void quotaExceededEndsQuietlyAndRetriesNextRun() {
        steamGame(1, "Escape the asylum.", 500);
        expectUsage(0, 500000);
        SERVER.expect(once(), requestTo("https://api-free.deepl.com/v2/translate"))
                .andRespond(withStatus(HttpStatusCode.valueOf(456)));

        assertJob(translationService.run(TriggerType.MANUAL), JobStatus.SUCCEEDED, 0, 0);
        SERVER.verify();
        assertThat(koAuto(1)).isNull();
    }

    private void expectUsage(long count, long limit) {
        SERVER.expect(once(), requestTo("https://api-free.deepl.com/v2/usage"))
                .andExpect(header("Authorization", "DeepL-Auth-Key test-key:fx"))
                .andRespond(withSuccess("{\"character_count\":" + count + ",\"character_limit\":" + limit + "}",
                        MediaType.APPLICATION_JSON));
    }

    private void steamGame(int appid, String description, int reviewCount) {
        steamGame(appid, description, reviewCount, false, game -> { });
    }

    private void steamGame(int appid, String description, int reviewCount, boolean adult) {
        steamGame(appid, description, reviewCount, adult, game -> { });
    }

    private void steamGame(int appid, String description, int reviewCount, Consumer<Game> customize) {
        steamGame(appid, description, reviewCount, false, customize);
    }

    private void steamGame(int appid, String description, int reviewCount, boolean adult, Consumer<Game> customize) {
        Game game = Game.candidateFromSteam(appid, "Game " + appid, "game-" + appid);
        game.applySteamData(new SteamGameData("Game " + appid, description, null, null, null, false, false,
                reviewCount, adult, List.of(), List.of()));
        customize.accept(game);
        gameRepository.save(game);
    }

    private String koAuto(int appid) {
        return jdbc.queryForObject("SELECT short_description_ko_auto FROM game WHERE external_id = ?", String.class,
                String.valueOf(appid));
    }

    private void assertJob(Long jobId, JobStatus status, int processed, int failed) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(JobType.TRANSLATE);
        assertThat(job.getStatus()).isEqualTo(status);
        assertThat(job.getProcessedCount()).isEqualTo(processed);
        assertThat(job.getFailedCount()).isEqualTo(failed);
    }
}
