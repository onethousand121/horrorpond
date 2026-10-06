package com.horrorpond.ingestion.api;

import com.horrorpond.ingestion.application.IngestionPipeline;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ADMIN_API_KEY=" + IngestionAdminApiTest.KEY)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IngestionAdminApiTest {

    static final String KEY = "test-admin-key";
    private static final String RUN_BODY = "{\"steps\":[\"DISCOVERY\",\"ENRICHMENT\",\"NORMALIZE\"]}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IngestionPipeline pipeline;

    @Autowired
    LockProvider lockProvider;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        DatabaseCleaner.clean(jdbc);
    }

    // ===== AdminKeyFilter =====

    @Test
    void missingKeyIs403() throws Exception {
        mvc.perform(runRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void wrongKeyIs403() throws Exception {
        mvc.perform(runRequest().header("X-Admin-Key", "wrong-key")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ingestion/jobs").header("X-Admin-Key", KEY + "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void correctKeyIs202AndRunsPipelineAsync() throws Exception {
        mvc.perform(runRequest().header("X-Admin-Key", KEY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.steps", hasSize(3)));

        verify(pipeline, timeout(5000)).runSteps(
                eq(List.of(JobType.DISCOVERY, JobType.ENRICHMENT, JobType.NORMALIZE)), eq(TriggerType.MANUAL));
    }

    @Test
    void nonAdminPathIsNotFiltered() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ===== API =====

    @Test
    void runIs409WhileLockIsHeld() throws Exception {
        SimpleLock lock = lockProvider.lock(new LockConfiguration(Instant.now(), IngestionPipeline.LOCK_NAME,
                Duration.ofMinutes(1), Duration.ZERO)).orElseThrow();
        try {
            mvc.perform(runRequest().header("X-Admin-Key", KEY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        } finally {
            lock.unlock();
        }
    }

    @Test
    void emptyStepsIs400() throws Exception {
        mvc.perform(post("/api/admin/ingestion/run").header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"steps\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void jobsReturnsMostRecentFirstWithLimit() throws Exception {
        Instant now = Instant.now();
        jobRepository.save(IngestionJob.start(JobType.DISCOVERY, TriggerType.SCHEDULED, now));
        jobRepository.save(IngestionJob.start(JobType.ENRICHMENT, TriggerType.SCHEDULED, now));
        jobRepository.save(IngestionJob.start(JobType.NORMALIZE, TriggerType.MANUAL, now));

        mvc.perform(get("/api/admin/ingestion/jobs").param("limit", "2").header("X-Admin-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("NORMALIZE"))
                .andExpect(jsonPath("$[0].status").value("RUNNING"))
                .andExpect(jsonPath("$[1].type").value("ENRICHMENT"));
    }

    @Test
    void addSeedCreatesManualSeedAndRejectsDuplicate() throws Exception {
        mvc.perform(post("/api/admin/ingestion/seeds").header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"appid\":739630}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appid").value(739630))
                .andExpect(jsonPath("$.discoveredBy").value("MANUAL"))
                .andExpect(jsonPath("$.fetchStatus").value("PENDING"));

        mvc.perform(post("/api/admin/ingestion/seeds").header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"appid\":739630}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSeedIs400() throws Exception {
        mvc.perform(post("/api/admin/ingestion/seeds").header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"appid\":-1}"))
                .andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder runRequest() {
        return post("/api/admin/ingestion/run").contentType(MediaType.APPLICATION_JSON).content(RUN_BODY);
    }
}
