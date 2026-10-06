package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SteamRawSnapshotRepository extends JpaRepository<SteamRawSnapshot, Integer> {
}
