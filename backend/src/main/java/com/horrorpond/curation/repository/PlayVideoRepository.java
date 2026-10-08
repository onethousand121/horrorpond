package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.PlayVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayVideoRepository extends JpaRepository<PlayVideo, Long> {

    List<PlayVideo> findByGameIdOrderBySortOrderAsc(Long gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PlayVideo v where v.gameId = :gameId")
    void deleteByGameId(Long gameId);
}
