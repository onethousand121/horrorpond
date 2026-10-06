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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IngestionPipelineIntegrationTest {

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
            game.publish(true, clock.instant());
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

    private void ingest(int... appids) {
        expectSteamSpy(appids);
        expectAppDetails(appids);
        discoveryService.run(TriggerType.MANUAL);
        enrichmentService.run(TriggerType.MANUAL);
        normalizeService.run(TriggerType.MANUAL);
        SERVER.verify();
        SERVER.reset();
    }

    private void expectSteamSpy(int... appids) {
        String body = Arrays.stream(appids)
                .mapToObj(appid -> "\"" + appid + "\":{\"appid\":" + appid + "}")
                .collect(Collectors.joining(",", "{", "}"));
        SERVER.expect(once(), requestTo(SteamMockServer.steamSpyHorrorUrl()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectAppDetails(int... appids) {
        for (int appid : appids) {
            SERVER.expect(once(), requestTo(SteamMockServer.appDetailsUrl(appid)))
                    .andRespond(withSuccess(Fixtures.appDetails(appid), MediaType.APPLICATION_JSON));
        }
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
