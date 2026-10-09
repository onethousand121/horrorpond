package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.ItchGameSeed;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ItchGameSeedRepository extends JpaRepository<ItchGameSeed, Long> {

    /**
     * 게임 페이지를 받을 차례인 seed. 관리자가 고른 게임 → 처음 받는 게임 → 오래된 갱신 → 실패 재시도 순.
     * 찾을 수 없던(NOT_FOUND) 게임도 갱신 주기마다 다시 확인한다 (일시적으로 비공개였을 수 있다).
     */
    @Query("""
            select s from ItchGameSeed s
            where s.fetchStatus = com.horrorpond.ingestion.domain.FetchStatus.PENDING
               or (s.fetchStatus in (com.horrorpond.ingestion.domain.FetchStatus.OK,
                                     com.horrorpond.ingestion.domain.FetchStatus.NOT_FOUND)
                   and s.lastFetchedAt < :refreshBefore)
               or (s.fetchStatus = com.horrorpond.ingestion.domain.FetchStatus.FAILED
                   and s.failCount < :maxFailCount and s.lastFetchedAt < :retryBefore)
            order by case when s.discoveredBy = com.horrorpond.ingestion.domain.ItchDiscoveredBy.MANUAL then 0 else 1 end,
                     case s.fetchStatus when com.horrorpond.ingestion.domain.FetchStatus.PENDING then 0
                                        when com.horrorpond.ingestion.domain.FetchStatus.FAILED then 2
                                        else 1 end,
                     s.lastFetchedAt asc nulls first, s.itchId asc
            """)
    List<ItchGameSeed> findFetchTargets(@Param("refreshBefore") Instant refreshBefore,
                                        @Param("retryBefore") Instant retryBefore,
                                        @Param("maxFailCount") int maxFailCount,
                                        Limit limit);
}
