package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.PlayVideo;
import com.horrorpond.curation.domain.VideoKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayVideoRepository extends JpaRepository<PlayVideo, Long> {

    List<PlayVideo> findByGameIdAndKindOrderBySortOrderAsc(Long gameId, VideoKind kind);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PlayVideo v where v.gameId = :gameId and v.kind = :kind")
    void deleteByGameIdAndKind(Long gameId, VideoKind kind);
}
