package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import com.horrorpond.ingestion.repository.SteamRawSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * enrichment 결과를 아이템 1건 단위 트랜잭션으로 저장한다.
 */
@Component
@RequiredArgsConstructor
public class EnrichmentItemWriter {

    private final SteamAppSeedRepository seedRepository;
    private final SteamRawSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional
    public void write(int appid, AppDetailsResult result) {
        SteamAppSeed seed = seedRepository.findById(appid).orElseThrow();
        Instant now = clock.instant();
        if (result instanceof AppDetailsResult.Found found) {
            saveSnapshot(appid, found.dataJson(), now);
            seed.markFetched(FetchStatus.OK, now);
        } else {
            seed.markFetched(FetchStatus.NOT_FOUND, now);
        }
    }

    @Transactional
    public void markFailed(int appid) {
        seedRepository.findById(appid).orElseThrow().markFailed(clock.instant());
    }

    private void saveSnapshot(int appid, String payload, Instant now) {
        String hash = sha256(payload);
        Optional<SteamRawSnapshot> existing = snapshotRepository.findById(appid);
        if (existing.isEmpty()) {
            snapshotRepository.save(SteamRawSnapshot.of(appid, payload, hash, now));
        } else if (!existing.get().getPayloadHash().equals(hash)) {
            existing.get().replace(payload, hash, now);
        } else {
            existing.get().refreshFetchedAt(now);
        }
    }

    static String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
