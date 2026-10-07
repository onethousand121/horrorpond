package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.SteamSearchList;
import com.horrorpond.ingestion.client.SteamSpyClient;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 공포게임 appid 중 seed에 없는 것만 추가한다. 기존 seed는 삭제하지 않는다.
 * <ul>
 *   <li>SteamSpy Horror 태그 목록: 전체 목록이지만 갱신이 느려 신작이 몇 달 늦게 들어온다</li>
 *   <li>Steam 스토어 검색: 최신 출시작과 인기 출시 예정작을 바로 잡는다</li>
 * </ul>
 * 한 출처가 실패해도 나머지는 저장하고, 실패 내용은 job 결과에 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private static final int CHUNK_SIZE = 500;
    static final int SEARCH_PAGE_SIZE = 100;
    /** 최신 출시작은 매일 이만큼 본다 (Horror 태그 신작은 하루 수십 개 수준) */
    static final int NEW_RELEASE_LIMIT = 300;
    /** 출시 예정작은 자동 노출되므로 인기 상위만 받는다 (전체는 2천 개가 넘고 대부분 무명) */
    static final int UPCOMING_LIMIT = 200;

    private final SteamSpyClient steamSpyClient;
    private final SteamStoreClient storeClient;
    private final SteamAppSeedRepository seedRepository;
    private final DiscoveryWriter writer;
    private final IngestionJobRecorder jobRecorder;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.DISCOVERY, trigger);
        try {
            Set<Integer> existing = new HashSet<>(seedRepository.findAllAppids());
            List<String> failures = new ArrayList<>();
            int added = 0;
            // 검색을 먼저: 같은 appid가 양쪽에 있으면 신작 우선순위를 받도록 STEAM_SEARCH로 들어간다
            added += discover("Steam search", DiscoveredBy.STEAM_SEARCH, this::searchHorrorAppIds, existing, failures);
            added += discover("SteamSpy", DiscoveredBy.STEAMSPY_TAG, steamSpyClient::fetchHorrorAppIds, existing,
                    failures);
            if (failures.isEmpty()) {
                jobRecorder.succeed(jobId, added, 0);
            } else {
                jobRecorder.fail(jobId, "added=" + added + ", " + String.join("; ", failures));
            }
        } catch (RuntimeException e) {
            log.error("Discovery failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    private int discover(String source, DiscoveredBy by, Supplier<Set<Integer>> fetch, Set<Integer> existing,
                         List<String> failures) {
        Set<Integer> discovered;
        try {
            discovered = fetch.get();
        } catch (RuntimeException e) {
            log.warn("Discovery from {} failed", source, e);
            failures.add(source + ": " + IngestionJobRecorder.describe(e));
            return 0;
        }
        List<Integer> newAppids = discovered.stream()
                .filter(appid -> !existing.contains(appid))
                .sorted()
                .toList();
        for (int from = 0; from < newAppids.size(); from += CHUNK_SIZE) {
            writer.insertSeeds(newAppids.subList(from, Math.min(from + CHUNK_SIZE, newAppids.size())), by);
        }
        existing.addAll(newAppids);
        log.info("Discovery from {} finished: discovered={}, new={}", source, discovered.size(), newAppids.size());
        return newAppids.size();
    }

    Set<Integer> searchHorrorAppIds() {
        Set<Integer> appids = new LinkedHashSet<>();
        appids.addAll(searchPages(SteamSearchList.NEW_RELEASES, NEW_RELEASE_LIMIT));
        appids.addAll(searchPages(SteamSearchList.POPULAR_UPCOMING, UPCOMING_LIMIT));
        return appids;
    }

    private List<Integer> searchPages(SteamSearchList list, int limit) {
        List<Integer> appids = new ArrayList<>();
        for (int start = 0; start < limit; start += SEARCH_PAGE_SIZE) {
            List<Integer> page = storeClient.fetchHorrorSearchPage(list, start, SEARCH_PAGE_SIZE);
            appids.addAll(page);
            if (page.size() < SEARCH_PAGE_SIZE) {
                break;
            }
        }
        return appids;
    }
}
