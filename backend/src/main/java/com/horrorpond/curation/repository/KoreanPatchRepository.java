package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.KoreanPatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KoreanPatchRepository extends JpaRepository<KoreanPatch, Long> {

    List<KoreanPatch> findByGameIdOrderBySortOrderAsc(Long gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from KoreanPatch p where p.gameId = :gameId")
    void deleteByGameId(Long gameId);
}
