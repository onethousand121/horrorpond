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

    @Transactional
    public SteamAppSeed addManual(int appid) {
        if (seedRepository.existsById(appid)) {
            throw new DomainStateException("Seed already exists: appid=" + appid);
        }
        return seedRepository.save(SteamAppSeed.discovered(appid, DiscoveredBy.MANUAL, clock.instant()));
    }
}
