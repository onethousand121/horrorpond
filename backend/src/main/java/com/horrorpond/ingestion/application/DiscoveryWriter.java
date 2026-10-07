package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * seed 신규 추가는 수천 건이 될 수 있어 1건이 아니라 청크 단위로 커밋한다 (외부 호출 없음).
 */
@Component
@RequiredArgsConstructor
public class DiscoveryWriter {

    private final SteamAppSeedRepository seedRepository;
    private final Clock clock;

    @Transactional
    public void insertSeeds(List<Integer> appids, DiscoveredBy by) {
        Instant now = clock.instant();
        seedRepository.saveAll(appids.stream()
                .map(appid -> SteamAppSeed.discovered(appid, by, now))
                .toList());
    }
}
