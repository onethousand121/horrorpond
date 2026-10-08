package com.horrorpond.catalog.repository;

import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.domain.GameMetricDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface GameMetricDailyRepository extends JpaRepository<GameMetricDaily, GameMetricDailyId> {

    /**
     * 그날 기록이 이미 있으면 덮어쓴다 (하루 1건, 마지막 값).
     * 같은 트랜잭션의 엔티티 변경(새 게임 등)을 먼저 flush하고, 영속성 컨텍스트는 비우지 않는다
     * (호출한 쪽이 이어서 다른 엔티티를 바꾼다).
     *
     * @param store {@link com.horrorpond.catalog.domain.Store} 이름 (STEAM, ITCH)
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO game_metric_daily (game_id, store, captured_on, review_count, captured_at)
            VALUES (:gameId, :store, :capturedOn, :reviewCount, :capturedAt)
            ON CONFLICT (game_id, store, captured_on)
            DO UPDATE SET review_count = EXCLUDED.review_count, captured_at = EXCLUDED.captured_at""")
    void upsert(@Param("gameId") long gameId, @Param("store") String store, @Param("capturedOn") LocalDate capturedOn,
                @Param("reviewCount") int reviewCount, @Param("capturedAt") Instant capturedAt);

    /**
     * 오늘 아직 Steam 리뷰 수를 기록하지 않은 게임 중 트렌드를 볼 만한 것.
     * 숨김·성인·출시 전(리뷰가 없음)은 빼고, 최근 출시작(recentSince 이후)을 먼저, 그다음 리뷰가 minReviews 이상인 게임을
     * 기록한 지 오래된 순으로 고른다. 그래서 한 번에 다 못 하는 게임은 날마다 돌아가며 기록된다.
     */
    @Query(nativeQuery = true, value = """
            SELECT g.id AS gameId, g.external_id AS appid
            FROM game g
            LEFT JOIN LATERAL (
                SELECT m.captured_on AS last_on
                FROM game_metric_daily m
                WHERE m.game_id = g.id AND m.store = 'STEAM'
                ORDER BY m.captured_on DESC
                LIMIT 1
            ) last ON true
            WHERE g.source = 'STEAM'
              AND g.status <> 'HIDDEN'
              AND g.adult = false
              AND g.coming_soon = false
              AND (g.release_date >= :recentSince OR g.review_count >= :minReviews)
              AND (last.last_on IS NULL OR last.last_on < :today)
            ORDER BY CASE WHEN g.release_date >= :recentSince THEN 0 ELSE 1 END,
                     last.last_on ASC NULLS FIRST,
                     g.review_count DESC NULLS LAST,
                     g.id
            LIMIT :limit""")
    List<SteamMetricTarget> findSteamMetricTargets(@Param("today") LocalDate today,
                                                   @Param("recentSince") LocalDate recentSince,
                                                   @Param("minReviews") int minReviews,
                                                   @Param("limit") int limit);

    interface SteamMetricTarget {

        Long getGameId();

        String getAppid();
    }
}
