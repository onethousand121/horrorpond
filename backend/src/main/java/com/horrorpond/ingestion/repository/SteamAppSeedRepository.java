package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface SteamAppSeedRepository extends JpaRepository<SteamAppSeed, Integer> {

    @Query("select s.appid from SteamAppSeed s")
    Set<Integer> findAllAppids();

    List<SteamAppSeed> findByFetchStatusOrderByDiscoveredAtAscAppidAsc(FetchStatus status, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, Instant fetchedBefore, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndFailCountLessThanAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, int maxFailCount, Instant fetchedBefore, Limit limit);
}
