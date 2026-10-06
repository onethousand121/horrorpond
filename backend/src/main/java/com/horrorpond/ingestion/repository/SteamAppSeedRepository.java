package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.HorrorTag;
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

    List<SteamAppSeed> findByFetchStatusAndDiscoveredByOrderByDiscoveredAtAscAppidAsc(
            FetchStatus status, DiscoveredBy discoveredBy, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
            FetchStatus status, DiscoveredBy discoveredBy, HorrorTag excluded, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndHorrorTagNotAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, HorrorTag excluded, Instant fetchedBefore, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndHorrorTagNotAndFailCountLessThanAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, HorrorTag excluded, int maxFailCount, Instant fetchedBefore, Limit limit);
}
