package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.SteamAppSeed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SteamAppSeedRepository extends JpaRepository<SteamAppSeed, Integer> {
}
