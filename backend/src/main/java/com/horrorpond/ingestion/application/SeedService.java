package com.horrorpond.ingestion.application;

import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final SteamAppSeedRepository seedRepository;
    private final Clock clock;

    /**
     * 수동 추가. 이미 발견만 된 게임이면 수동 요청으로 바꿔 다음 수집에서 가장 먼저 받는다.
     * 이미 수집된 게임이면 409 (관리 화면에서 찾아 고정 노출하면 된다).
     */
    @Transactional
    public SteamAppSeed addManual(int appid) {
        return seedRepository.findById(appid)
                .map(seed -> {
                    seed.requestManually();
                    return seed;
                })
                .orElseGet(() -> seedRepository.save(SteamAppSeed.discovered(appid, DiscoveredBy.MANUAL,
                        clock.instant())));
    }
}
