package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.EndingGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EndingGuideRepository extends JpaRepository<EndingGuide, Long> {

    List<EndingGuide> findByGameIdOrderBySortOrderAsc(Long gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EndingGuide e where e.gameId = :gameId")
    void deleteByGameId(Long gameId);
}
