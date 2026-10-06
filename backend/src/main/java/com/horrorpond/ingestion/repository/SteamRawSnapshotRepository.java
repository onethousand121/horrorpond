package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SteamRawSnapshotRepository extends JpaRepository<SteamRawSnapshot, Integer> {

    @Query("""
            select s.appid from SteamRawSnapshot s
            where s.normalizedHash is null or s.normalizedHash <> s.payloadHash
            order by s.appid""")
    List<Integer> findAppidsNeedingNormalize();
}
