package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.SteamSpyClient;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * SteamSpy Horror 태그의 appid 중 seed에 없는 것만 추가한다. 기존 seed는 삭제하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private static final int CHUNK_SIZE = 500;

    private final SteamSpyClient steamSpyClient;
    private final SteamAppSeedRepository seedRepository;
    private final DiscoveryWriter writer;
    private final IngestionJobRecorder jobRecorder;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.DISCOVERY, trigger);
        try {
            Set<Integer> discovered = steamSpyClient.fetchHorrorAppIds();
            Set<Integer> existing = seedRepository.findAllAppids();
            List<Integer> newAppids = discovered.stream()
                    .filter(appid -> !existing.contains(appid))
                    .sorted()
                    .toList();
            for (int from = 0; from < newAppids.size(); from += CHUNK_SIZE) {
                writer.insertSeeds(newAppids.subList(from, Math.min(from + CHUNK_SIZE, newAppids.size())));
            }
            log.info("Discovery finished: discovered={}, new={}", discovered.size(), newAppids.size());
            jobRecorder.succeed(jobId, newAppids.size(), 0);
        } catch (RuntimeException e) {
            log.error("Discovery failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }
}
