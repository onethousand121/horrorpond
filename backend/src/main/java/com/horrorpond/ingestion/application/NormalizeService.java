package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamRawSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 아직 반영하지 않은(normalized_hash ≠ payload_hash) 스냅샷을 Game으로 반영한다. 재실행해도 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizeService {

    private final SteamRawSnapshotRepository snapshotRepository;
    private final NormalizeItemProcessor itemProcessor;
    private final IngestionJobRecorder jobRecorder;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.NORMALIZE, trigger);
        try {
            List<Integer> appids = snapshotRepository.findAppidsNeedingNormalize();
            int processed = 0;
            int failed = 0;
            for (int appid : appids) {
                jobRecorder.progress(jobId, processed, failed);
                try {
                    itemProcessor.process(appid);
                    processed++;
                } catch (RuntimeException e) {
                    log.warn("Normalize failed: appid={}, {}", appid, IngestionJobRecorder.describe(e));
                    failed++;
                }
            }
            log.info("Normalize finished: targets={}, processed={}, failed={}", appids.size(), processed, failed);
            jobRecorder.succeed(jobId, processed, failed);
        } catch (RuntimeException e) {
            log.error("Normalize failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }
}
