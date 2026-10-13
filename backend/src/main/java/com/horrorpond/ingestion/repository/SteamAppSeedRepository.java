package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.HorrorTag;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface SteamAppSeedRepository extends JpaRepository<SteamAppSeed, Integer> {

    @Query("select s.appid from SteamAppSeed s")
    Set<Integer> findAllAppids();

    List<SteamAppSeed> findByFetchStatusAndDiscoveredByOrderByDiscoveredAtAscAppidAsc(
            FetchStatus status, DiscoveredBy discoveredBy, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
            FetchStatus status, DiscoveredBy discoveredBy, HorrorTag excluded, Limit limit);

    /**
     * 출시 무렵이라 상세가 자주 바뀌는 게임: 아직 "출시 예정"인데 출시일이 [releaseFrom, releaseTo] 안에 있는(지났거나 곧인) 것.
     * 출시·연기·날짜 확정이 이때 몰리므로 7일 갱신 주기를 기다리지 않고 매일 다시 받는다. 숨긴 게임은 뺀다.
     */
    @Query(nativeQuery = true, value = """
            SELECT s.* FROM steam_app_seed s
            JOIN game g ON g.source = 'STEAM' AND g.external_id = CAST(s.appid AS VARCHAR)
            WHERE s.fetch_status = 'OK'
              AND g.coming_soon
              AND g.status <> 'HIDDEN'
              AND g.release_date BETWEEN :releaseFrom AND :releaseTo
              AND s.last_fetched_at < :fetchedBefore
            ORDER BY g.release_date, s.appid
            LIMIT :limit""")
    List<SteamAppSeed> findReleaseWindowRefreshTargets(@Param("releaseFrom") LocalDate releaseFrom,
                                                        @Param("releaseTo") LocalDate releaseTo,
                                                        @Param("fetchedBefore") Instant fetchedBefore,
                                                        @Param("limit") int limit);

    List<SteamAppSeed> findByFetchStatusAndHorrorTagNotAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, HorrorTag excluded, Instant fetchedBefore, Limit limit);

    List<SteamAppSeed> findByFetchStatusAndHorrorTagNotAndFailCountLessThanAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
            FetchStatus status, HorrorTag excluded, int maxFailCount, Instant fetchedBefore, Limit limit);
}
