package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.AchievementGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AchievementGuideRepository extends JpaRepository<AchievementGuide, Long> {

    List<AchievementGuide> findByGameIdOrderBySortOrderAsc(Long gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AchievementGuide g where g.gameId = :gameId")
    void deleteByGameId(Long gameId);
}
