package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.support.JpaSliceTest;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JpaSliceTest
class IngestionMappingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String PAYLOAD = """
            {"name": "Outlast", "steam_appid": 238320, "genres": [{"id": "1", "description": "Action"}]}""";

    @Autowired
    SteamAppSeedRepository seedRepository;

    @Autowired
    SteamRawSnapshotRepository snapshotRepository;

    @Autowired
    IngestionJobRepository jobRepository;

    @Autowired
    EntityManager em;

    @Test
    void seedRoundTrip() {
        seedRepository.save(SteamAppSeed.discovered(238320, DiscoveredBy.STEAMSPY_TAG, NOW));
        flushAndClear();

        SteamAppSeed found = seedRepository.findById(238320).orElseThrow();
        assertThat(found.isNew()).isFalse();
        assertThat(found.getDiscoveredBy()).isEqualTo(DiscoveredBy.STEAMSPY_TAG);
        assertThat(found.getDiscoveredAt()).isEqualTo(NOW);
        assertThat(found.getFetchStatus()).isEqualTo(FetchStatus.PENDING);

        found.markFailed(NOW.plusSeconds(10));
        seedRepository.save(found);
        flushAndClear();

        SteamAppSeed failed = seedRepository.findById(238320).orElseThrow();
        assertThat(failed.getFetchStatus()).isEqualTo(FetchStatus.FAILED);
        assertThat(failed.getFailCount()).isEqualTo(1);
        assertThat(failed.getLastFetchedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void snapshotPayloadRoundTripsAsJsonb() {
        snapshotRepository.save(SteamRawSnapshot.of(238320, PAYLOAD, HASH_A, NOW));
        flushAndClear();

        SteamRawSnapshot found = snapshotRepository.findById(238320).orElseThrow();
        JsonMapper json = JsonMapper.builder().build();
        assertThat(json.readTree(found.getPayload())).isEqualTo(json.readTree(PAYLOAD));
        assertThat(found.getPayloadHash()).isEqualTo(HASH_A);
        assertThat(found.needsNormalize()).isTrue();

        Object[] row = (Object[]) em.createNativeQuery(
                        "select jsonb_typeof(payload), payload->>'name' from steam_raw_snapshot where appid = 238320")
                .getSingleResult();
        assertThat(row[0]).isEqualTo("object");
        assertThat(row[1]).isEqualTo("Outlast");

        found.markNormalized();
        found.replace(PAYLOAD, HASH_B, NOW.plusSeconds(60));
        snapshotRepository.save(found);
        flushAndClear();

        SteamRawSnapshot replaced = snapshotRepository.findById(238320).orElseThrow();
        assertThat(replaced.getNormalizedHash()).isEqualTo(HASH_A);
        assertThat(replaced.needsNormalize()).isTrue();
    }

    @Test
    void jobRoundTrip() {
        IngestionJob job = IngestionJob.start(JobType.ENRICHMENT, TriggerType.MANUAL, NOW);
        job.succeed(10, 2, NOW.plusSeconds(30));
        Long id = jobRepository.save(job).getId();
        flushAndClear();

        IngestionJob found = jobRepository.findById(id).orElseThrow();
        assertThat(found.getType()).isEqualTo(JobType.ENRICHMENT);
        assertThat(found.getTriggerType()).isEqualTo(TriggerType.MANUAL);
        assertThat(found.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(found.getProcessedCount()).isEqualTo(10);
        assertThat(found.getFailedCount()).isEqualTo(2);
        assertThat(found.getFinishedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void savingNewSeedIssuesInsertOnlyWithoutSelect() {
        Statistics stats = statistics();
        stats.clear();

        seedRepository.save(SteamAppSeed.discovered(1001, DiscoveredBy.MANUAL, NOW));
        em.flush();

        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void savingNewSnapshotIssuesInsertOnlyWithoutSelect() {
        Statistics stats = statistics();
        stats.clear();

        snapshotRepository.save(SteamRawSnapshot.of(1001, PAYLOAD, HASH_A, NOW));
        em.flush();

        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    /**
     * 대조군: Persistable 없이 merge로 저장하면 select가 먼저 나간다는 것을 통계로 확인한다.
     */
    @Test
    void mergingNewSeedIssuesSelectBeforeInsert() {
        Statistics stats = statistics();
        stats.clear();

        em.merge(SteamAppSeed.discovered(1002, DiscoveredBy.MANUAL, NOW));
        em.flush();

        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    private Statistics statistics() {
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        assertThat(stats.isStatisticsEnabled()).isTrue();
        return stats;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
