package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameDeveloper;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.repository.DeveloperRepository;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.common.domain.Language;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.HorrorTag;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IngestionPipelineIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Duration RATE_LIMIT_WAIT = Duration.ofSeconds(60);
    private static final MockRestServiceServer SERVER = SteamMockServer.SERVER;

    @TestBean(name = "steamStoreRestClient", methodName = "com.horrorpond.support.SteamMockServer#storeRestClient")
    RestClient steamStoreRestClient;

    @TestBean(name = "steamSpyRestClient", methodName = "com.horrorpond.support.SteamMockServer#spyRestClient")
    RestClient steamSpyRestClient;

    @MockitoBean
    Sleeper sleeper;

    @Autowired
    DiscoveryService discoveryService;

    @Autowired
    EnrichmentService enrichmentService;

    @Autowired
    NormalizeService normalizeService;

    @Autowired
    StaleJobCleaner staleJobCleaner;

    @Autowired
    SteamAppSeedRepository seedRepository;

    @Autowired
    SteamRawSnapshotRepository snapshotRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GenreRepository genreRepository;

    @Autowired
    DeveloperRepository developerRepository;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

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
    void fullPipelineCreatesCandidateGamesWithDevelopers() {
        expectSteamSpy(1262350, 739630, 594330, 5060920, 1);
        expectHorrorTags(1262350, 739630, 594330, 5060920, 1);
        expectAppDetails(1262350, 739630, 594330, 5060920, 1);

        assertJob(discoveryService.run(TriggerType.MANUAL), JobType.DISCOVERY, JobStatus.SUCCEEDED, 5, 0);
        assertJob(enrichmentService.run(TriggerType.MANUAL), JobType.ENRICHMENT, JobStatus.SUCCEEDED, 5, 0);
        assertJob(normalizeService.run(TriggerType.MANUAL), JobType.NORMALIZE, JobStatus.SUCCEEDED, 4, 0);
        SERVER.verify();

        assertThat(seedRepository.findAll())
                .extracting(SteamAppSeed::getAppid, SteamAppSeed::getFetchStatus)
                .containsExactlyInAnyOrder(
                        tuple(1262350, FetchStatus.OK), tuple(739630, FetchStatus.OK),
                        tuple(594330, FetchStatus.OK), tuple(5060920, FetchStatus.OK),
                        tuple(1, FetchStatus.NOT_FOUND));
        assertThat(snapshotRepository.count()).isEqualTo(4);

        tx.executeWithoutResult(status -> {
            Game phasmo = steamGame(739630);
            assertThat(phasmo.getStatus()).isEqualTo(GameStatus.CANDIDATE);
            assertThat(phasmo.getTitle()).isEqualTo("Phasmophobia");
            assertThat(phasmo.getSlug()).isEqualTo("phasmophobia-739630");
            assertThat(phasmo.getMedia()).hasSize(7 + 48);
            assertThat(phasmo.getTags()).containsExactly("Horror", "Online Co-Op", "Psychological Horror");
            assertThat(phasmo.getReviewCount()).isEqualTo(684837);
            assertThat(phasmo.isAdult()).isFalse();
            assertThat(phasmo.getDevelopers())
                    .extracting(gd -> gd.getDeveloper().getName(), GameDeveloper::getRole)
                    .containsExactlyInAnyOrder(tuple("Kinetic Games", DeveloperRole.DEVELOPER),
                            tuple("Kinetic Games", DeveloperRole.PUBLISHER));

            Game signalis = steamGame(1262350);
            assertThat(signalis.getDevelopers())
                    .extracting(gd -> gd.getDeveloper().getName(), GameDeveloper::getRole)
                    .containsExactlyInAnyOrder(tuple("rose-engine", DeveloperRole.DEVELOPER),
                            tuple("Balor Games", DeveloperRole.PUBLISHER));
            assertThat(steamGame(594330).getStatus()).isEqualTo(GameStatus.CANDIDATE);
        });
        assertThat(gameRepository.findBySourceAndExternalId(GameSource.STEAM, "5060920"))
                .as("dlc는 Game을 만들지 않는다").isEmpty();
        assertThat(gameRepository.count()).isEqualTo(3);
        assertThat(developerRepository.count()).isEqualTo(4);
        assertThat(developerRepository.findByName("rose-engine").orElseThrow().getSlug()).isEqualTo("rose-engine");
    }

    @Test
    void gamesWithoutHorrorInTopTagsAreExcludedBeforeSteamCall() {
        // PUBG(578080)는 SteamSpy Horror 태그 목록에 있지만 상위 태그에 Horror가 없다
        expectSteamSpy(739630, 578080);
        expectHorrorTags(739630);
        expectTags(578080, "Survival", "Shooter", "Battle Royale");
        expectAppDetails(739630);

        discoveryService.run(TriggerType.MANUAL);
        assertJob(enrichmentService.run(TriggerType.MANUAL), JobType.ENRICHMENT, JobStatus.SUCCEEDED, 2, 0);
        SERVER.verify();

        SteamAppSeed pubg = seedRepository.findById(578080).orElseThrow();
        assertThat(pubg.getHorrorTag()).isEqualTo(HorrorTag.NOT_HORROR);
        assertThat(pubg.getHorrorTagCheckedAt()).isNotNull();
        assertThat(pubg.getFetchStatus()).isEqualTo(FetchStatus.PENDING);
        assertThat(snapshotRepository.existsById(578080)).isFalse();
        assertThat(seedRepository.findById(739630).orElseThrow().getHorrorTag()).isEqualTo(HorrorTag.HORROR);

        // 다음 실행부터는 판정을 다시 하지 않고 대상에서도 빠진다
        assertThat(enrichmentService.selectTargets()).isEmpty();
    }

    @Test
    void steamSearchFindsNewGamesThatSteamSpyDoesNotListYet() {
        expectSearch(new int[]{1262350, 739630}, new int[]{594330});
        String spyBody = "{\"739630\":{\"appid\":739630},\"5060920\":{\"appid\":5060920}}";
        SERVER.expect(once(), requestTo(SteamMockServer.steamSpyHorrorUrl()))
                .andRespond(withSuccess(spyBody, MediaType.APPLICATION_JSON));

        assertJob(discoveryService.run(TriggerType.MANUAL), JobType.DISCOVERY, JobStatus.SUCCEEDED, 4, 0);
        SERVER.verify();

        // 양쪽에 다 있는 appid는 검색(신작 우선순위)으로 들어간다
        assertThat(seedRepository.findAll())
                .extracting(SteamAppSeed::getAppid, SteamAppSeed::getDiscoveredBy)
                .containsExactlyInAnyOrder(
                        tuple(1262350, DiscoveredBy.STEAM_SEARCH), tuple(739630, DiscoveredBy.STEAM_SEARCH),
                        tuple(594330, DiscoveredBy.STEAM_SEARCH), tuple(5060920, DiscoveredBy.STEAMSPY_TAG));
        // 검색으로 발견한 seed도 SteamSpy 상위 태그로 공포 판정을 받는다
        assertThat(seedRepository.findById(594330).orElseThrow().needsHorrorTagCheck()).isTrue();
    }

    @Test
    void steamSearchFailureStillSavesSteamSpySeeds() {
        SERVER.expect(ExpectedCount.times(3), requestTo(SteamMockServer.steamSearchUrl("sort_by=Released_DESC", 0)))
                .andRespond(withServerError());
        SERVER.expect(once(), requestTo(SteamMockServer.steamSpyHorrorUrl()))
                .andRespond(withSuccess("{\"739630\":{\"appid\":739630}}", MediaType.APPLICATION_JSON));

        Long jobId = discoveryService.run(TriggerType.MANUAL);
        SERVER.verify();

        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).startsWith("added=1, Steam search:");
        assertThat(seedRepository.findAll()).extracting(SteamAppSeed::getAppid).containsExactly(739630);
    }

    @Test
    void englishTextIsStoredAndNamesTheSlug() {
        seedRepository.save(SteamAppSeed.discovered(739630, DiscoveredBy.MANUAL, clock.instant()));
        expectTags(739630, "Horror");
        String korean = Fixtures.appDetails(739630).replace("\"name\":\"Phasmophobia\"", "\"name\":\"파스모포비아\"");
        SERVER.expect(once(), requestTo(SteamMockServer.appDetailsUrl(739630)))
                .andRespond(withSuccess(korean, MediaType.APPLICATION_JSON));
        expectEnglish(739630, "Phasmophobia", "Ghost hunting <b>co-op</b>", "Sep 18, 2020");

        enrichmentService.run(TriggerType.MANUAL);
        normalizeService.run(TriggerType.MANUAL);
        SERVER.verify();

        tx.executeWithoutResult(status -> {
            Game game = steamGame(739630);
            assertThat(game.getTitle()).isEqualTo("파스모포비아");
            assertThat(game.getSlug()).as("한국어 이름 대신 영어 이름으로 주소를 만든다").isEqualTo("phasmophobia-739630");
            assertThat(game.title(Language.EN)).isEqualTo("Phasmophobia");
            assertThat(game.shortDescription(Language.EN)).isEqualTo("Ghost hunting co-op");
            assertThat(game.releaseDateText(Language.EN)).isEqualTo("Sep 18, 2020");
        });
    }

    @Test
    void manualSeedsGetTagsButSkipHorrorCheck() {
        seedRepository.save(SteamAppSeed.discovered(739630, DiscoveredBy.MANUAL, clock.instant()));
        expectTags(739630, "Online Co-Op", "Investigation");
        expectAppDetails(739630);

        assertJob(enrichmentService.run(TriggerType.MANUAL), JobType.ENRICHMENT, JobStatus.SUCCEEDED, 1, 0);
        SERVER.verify();
        SteamAppSeed seed = seedRepository.findById(739630).orElseThrow();
        // Horror 태그가 없어도 수동 추가라 판정하지 않고 수집한다
        assertThat(seed.getHorrorTag()).isEqualTo(HorrorTag.UNCHECKED);
        assertThat(seed.getSpyTags()).containsExactly("Online Co-Op", "Investigation");
        assertThat(seed.getFetchStatus()).isEqualTo(FetchStatus.OK);
    }

    @Test
    void normalizeRerunWithSamePayloadProcessesNothing() {
        ingest(739630);

        assertJob(normalizeService.run(TriggerType.MANUAL), JobType.NORMALIZE, JobStatus.SUCCEEDED, 0, 0);
    }

    @Test
    void changedPayloadUpdatesSteamFieldsButKeepsCuratorSlugAndGenres() {
        ingest(739630);
        Long genreId = genreRepository.save(Genre.create("Co-op", "co-op", null, 0)).getId();
        tx.executeWithoutResult(status -> {
            Game game = steamGame(739630);
            game.changeSlug("phasmophobia");
            game.replaceGenres(Set.of(genreRepository.findById(genreId).orElseThrow()));
        });

        String changed = SteamAppDetailsParserTest.modified(739630, d -> d.put("name", "Phasmophobia Remastered"));
        tx.executeWithoutResult(status -> snapshotRepository.findById(739630).orElseThrow()
                .replace(changed, EnrichmentItemWriter.sha256(changed), clock.instant()));

        assertJob(normalizeService.run(TriggerType.MANUAL), JobType.NORMALIZE, JobStatus.SUCCEEDED, 1, 0);
        tx.executeWithoutResult(status -> {
            Game game = steamGame(739630);
            assertThat(game.getTitle()).isEqualTo("Phasmophobia Remastered");
            assertThat(game.getSlug()).isEqualTo("phasmophobia");
            assertThat(game.getGenres()).extracting(Genre::getSlug).containsExactly("co-op");
            assertThat(game.getDevelopers()).hasSize(2);
        });
    }

    @Test
    void renormalizeUpdatesCoopButKeepsCuratorSlugGenresAndStatus() {
        ingest(739630);
        Long genreId = genreRepository.save(Genre.create("Co-op", "co-op", null, 0)).getId();
        tx.executeWithoutResult(status -> {
            Game game = steamGame(739630);
            assertThat(game.isCoop()).as("fixture에 9/38 협동 카테고리").isTrue();
            game.changeSlug("phasmophobia");
            game.replaceGenres(Set.of(genreRepository.findById(genreId).orElseThrow()));
            game.publish(clock.instant());
        });

        String singlePlayerOnly = SteamAppDetailsParserTest.modified(739630, d ->
                d.putArray("categories").addObject().put("id", 2).put("description", "싱글 플레이어"));
        tx.executeWithoutResult(status -> snapshotRepository.findById(739630).orElseThrow()
                .replace(singlePlayerOnly, EnrichmentItemWriter.sha256(singlePlayerOnly), clock.instant()));

        assertJob(normalizeService.run(TriggerType.MANUAL), JobType.NORMALIZE, JobStatus.SUCCEEDED, 1, 0);
        tx.executeWithoutResult(status -> {
            Game game = steamGame(739630);
            assertThat(game.isCoop()).isFalse();
            assertThat(game.getSlug()).isEqualTo("phasmophobia");
            assertThat(game.getGenres()).extracting(Genre::getSlug).containsExactly("co-op");
            assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        });
    }

    @Test
    void rateLimitWaitsThenRetriesSameAppid() {
        seedRepository.save(SteamAppSeed.discovered(739630, DiscoveredBy.MANUAL, clock.instant()));
        expectHorrorTags(739630);
        SERVER.expect(once(), requestTo(SteamMockServer.appDetailsUrl(739630)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        expectAppDetails(739630);

        assertJob(enrichmentService.run(TriggerType.MANUAL), JobType.ENRICHMENT, JobStatus.SUCCEEDED, 1, 0);
        SERVER.verify();
        verify(sleeper, times(1)).sleep(eq(RATE_LIMIT_WAIT));
        assertThat(seedRepository.findById(739630).orElseThrow().getFetchStatus()).isEqualTo(FetchStatus.OK);
    }

    @Test
    void threeConsecutiveRateLimitsFailTheJob() {
        seedRepository.save(SteamAppSeed.discovered(739630, DiscoveredBy.MANUAL, clock.instant()));
        expectHorrorTags(739630);
        SERVER.expect(ExpectedCount.times(3),
                        requestTo(SteamMockServer.appDetailsUrl(739630)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Long jobId = enrichmentService.run(TriggerType.MANUAL);

        SERVER.verify();
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Rate limited 3 times in a row");
        verify(sleeper, times(2)).sleep(eq(RATE_LIMIT_WAIT));
        assertThat(seedRepository.findById(739630).orElseThrow().getFetchStatus()).isEqualTo(FetchStatus.PENDING);
    }

    @Test
    void enrichmentTargetsFollowPriorityOrder() {
        Instant now = clock.instant();
        // STEAMSPY PENDING: appid 내림차순이어야 한다
        saveSeed(100, DiscoveredBy.STEAMSPY_TAG, now.minus(Duration.ofDays(1)));
        saveSeed(300, DiscoveredBy.STEAMSPY_TAG, now.minus(Duration.ofDays(1)));
        saveSeed(200, DiscoveredBy.STEAMSPY_TAG, now.minus(Duration.ofDays(1)));
        // STEAM_SEARCH PENDING: 수동 다음, SteamSpy보다 먼저 (appid 내림차순)
        saveSeed(50, DiscoveredBy.STEAM_SEARCH, now);
        saveSeed(60, DiscoveredBy.STEAM_SEARCH, now);
        // MANUAL PENDING: appid와 무관하게 가장 먼저, 발견 순서대로
        saveSeed(999999, DiscoveredBy.MANUAL, now.minus(Duration.ofHours(2)));
        saveSeed(10, DiscoveredBy.MANUAL, now.minus(Duration.ofHours(1)));
        // OK: 갱신 주기(7일) 지난 것만
        SteamAppSeed staleOk = SteamAppSeed.discovered(400, DiscoveredBy.STEAMSPY_TAG, now);
        staleOk.markFetched(FetchStatus.OK, now.minus(Duration.ofDays(8)));
        SteamAppSeed freshOk = SteamAppSeed.discovered(401, DiscoveredBy.STEAMSPY_TAG, now);
        freshOk.markFetched(FetchStatus.OK, now.minus(Duration.ofDays(1)));
        // FAILED: 재시도 대기(1일) 지났고 실패 횟수 < 3 인 것만
        SteamAppSeed retryable = SteamAppSeed.discovered(500, DiscoveredBy.STEAMSPY_TAG, now);
        retryable.markFailed(now.minus(Duration.ofDays(2)));
        SteamAppSeed tooRecent = SteamAppSeed.discovered(501, DiscoveredBy.STEAMSPY_TAG, now);
        tooRecent.markFailed(now.minus(Duration.ofHours(1)));
        SteamAppSeed exhausted = SteamAppSeed.discovered(502, DiscoveredBy.STEAMSPY_TAG, now);
        exhausted.markFailed(now.minus(Duration.ofDays(4)));
        exhausted.markFailed(now.minus(Duration.ofDays(3)));
        exhausted.markFailed(now.minus(Duration.ofDays(2)));
        SteamAppSeed notFound = SteamAppSeed.discovered(600, DiscoveredBy.STEAMSPY_TAG, now);
        notFound.markFetched(FetchStatus.NOT_FOUND, now.minus(Duration.ofDays(30)));
        // NOT_HORROR: 상태와 무관하게 모두 제외
        SteamAppSeed notHorrorPending = SteamAppSeed.discovered(700, DiscoveredBy.STEAMSPY_TAG, now);
        notHorrorPending.recordHorrorTag(HorrorTag.NOT_HORROR, now);
        SteamAppSeed notHorrorStaleOk = SteamAppSeed.discovered(701, DiscoveredBy.STEAMSPY_TAG, now);
        notHorrorStaleOk.markFetched(FetchStatus.OK, now.minus(Duration.ofDays(8)));
        notHorrorStaleOk.recordHorrorTag(HorrorTag.NOT_HORROR, now);
        SteamAppSeed notHorrorRetryable = SteamAppSeed.discovered(702, DiscoveredBy.STEAMSPY_TAG, now);
        notHorrorRetryable.markFailed(now.minus(Duration.ofDays(2)));
        notHorrorRetryable.recordHorrorTag(HorrorTag.NOT_HORROR, now);
        seedRepository.saveAll(List.of(staleOk, freshOk, retryable, tooRecent, exhausted, notFound,
                notHorrorPending, notHorrorStaleOk, notHorrorRetryable));

        assertThat(enrichmentService.selectTargets())
                .extracting(SteamAppSeed::getAppid)
                .containsExactly(999999, 10, 60, 50, 300, 200, 100, 400, 500);
    }

    @Test
    void staleRunningJobsAreFailedOnStartup() {
        Instant now = clock.instant();
        Long staleId = jobRepository.save(IngestionJob.start(JobType.ENRICHMENT, TriggerType.SCHEDULED,
                now.minus(Duration.ofHours(3)))).getId();
        Long recentId = jobRepository.save(IngestionJob.start(JobType.ENRICHMENT, TriggerType.SCHEDULED,
                now.minus(Duration.ofHours(1)))).getId();

        staleJobCleaner.failStaleRunningJobs();

        IngestionJob stale = jobRepository.findById(staleId).orElseThrow();
        assertThat(stale.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(stale.getErrorMessage()).isEqualTo("interrupted by shutdown");
        assertThat(jobRepository.findById(recentId).orElseThrow().getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    private void saveSeed(int appid, DiscoveredBy by, Instant discoveredAt) {
        seedRepository.save(SteamAppSeed.discovered(appid, by, discoveredAt));
    }

    private void ingest(int... appids) {
        expectSteamSpy(appids);
        expectHorrorTags(appids);
        expectAppDetails(appids);
        discoveryService.run(TriggerType.MANUAL);
        enrichmentService.run(TriggerType.MANUAL);
        normalizeService.run(TriggerType.MANUAL);
        SERVER.verify();
        SERVER.reset();
    }

    private void expectSteamSpy(int... appids) {
        expectSearch(new int[0], new int[0]);
        String body = Arrays.stream(appids)
                .mapToObj(appid -> "\"" + appid + "\":{\"appid\":" + appid + "}")
                .collect(Collectors.joining(",", "{", "}"));
        SERVER.expect(once(), requestTo(SteamMockServer.steamSpyHorrorUrl()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectSearch(int[] newReleases, int[] upcoming) {
        SERVER.expect(once(), requestTo(SteamMockServer.steamSearchUrl("sort_by=Released_DESC", 0)))
                .andRespond(withSuccess(SteamMockServer.steamSearchBody(newReleases), MediaType.APPLICATION_JSON));
        SERVER.expect(once(), requestTo(SteamMockServer.steamSearchUrl("filter=popularcomingsoon", 0)))
                .andRespond(withSuccess(SteamMockServer.steamSearchBody(upcoming), MediaType.APPLICATION_JSON));
    }

    private void expectHorrorTags(int... appids) {
        for (int appid : appids) {
            expectTags(appid, "Horror", "Online Co-Op", "Psychological Horror");
        }
    }

    private void expectTags(int appid, String... tags) {
        String body = Arrays.stream(tags)
                .map(tag -> "\"" + tag + "\":100")
                .collect(Collectors.joining(",", "{\"appid\":" + appid + ",\"tags\":{", "}}"));
        SERVER.expect(once(), requestTo(SteamMockServer.steamSpyAppDetailsUrl(appid)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectAppDetails(int... appids) {
        for (int appid : appids) {
            String body = Fixtures.appDetails(appid);
            SERVER.expect(once(), requestTo(SteamMockServer.appDetailsUrl(appid)))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            JsonNode data = JSON.readTree(body).path(String.valueOf(appid)).path("data");
            if (data.isObject()) {
                // 영어 응답은 이름은 같고 소개만 영어인 것으로 흉내 낸다
                expectEnglish(appid, data.path("name").asString(), "English description " + appid, "Sep 18, 2020");
            }
        }
    }

    private void expectEnglish(int appid, String name, String shortDescription, String releaseDate) {
        String body = JSON.writeValueAsString(Map.of(String.valueOf(appid), Map.of("success", true, "data",
                Map.of("name", name, "short_description", shortDescription, "release_date",
                        Map.of("coming_soon", false, "date", releaseDate)))));
        SERVER.expect(once(), requestTo(SteamMockServer.appDetailsEnglishUrl(appid)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private Game steamGame(int appid) {
        return gameRepository.findBySourceAndExternalId(GameSource.STEAM, String.valueOf(appid)).orElseThrow();
    }

    private void assertJob(Long jobId, JobType type, JobStatus status, int processed, int failed) {
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getType()).isEqualTo(type);
        assertThat(job.getStatus()).as("job %s error=%s", type, job.getErrorMessage()).isEqualTo(status);
        assertThat(job.getProcessedCount()).isEqualTo(processed);
        assertThat(job.getFailedCount()).isEqualTo(failed);
        assertThat(job.getFinishedAt()).isNotNull();
    }
}
