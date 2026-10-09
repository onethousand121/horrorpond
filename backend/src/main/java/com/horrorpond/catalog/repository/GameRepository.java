package com.horrorpond.catalog.repository;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySourceAndExternalId(GameSource source, String externalId);

    boolean existsBySlug(String slug);

    /** 한국어·영어 이름 중 하나가 같은(대소문자 무시) 게임이 그 출처에 있는지 */
    @Query("""
            select count(g) > 0 from Game g
            where g.source = :source and (lower(g.title) = lower(:title) or lower(g.titleEn) = lower(:title))""")
    boolean existsBySourceAndTitle(@Param("source") GameSource source, @Param("title") String title);

    Optional<Game> findBySlug(String slug);

    /**
     * 자동 번역할 게임: 한국어 소개에 한글이 없고 아직 번역이 없는 것. 숨김·성인(상세에서 소개를 안 보여줌)은 뺀다.
     * 사이트에 보일 가능성이 큰 순서(큐레이터 공개 → 출시 예정·최근 출시 → 리뷰 많은 순)로 고른다.
     */
    @Query(nativeQuery = true, value = """
            SELECT g.id AS gameId, g.short_description AS shortDescription
            FROM game g
            WHERE g.short_description IS NOT NULL
              AND btrim(g.short_description) <> ''
              AND g.short_description_ko_auto IS NULL
              AND g.short_description !~ '[가-힣]'
              AND g.status <> 'HIDDEN'
              AND g.adult = false
            ORDER BY (g.status = 'PUBLISHED') DESC,
                     (g.coming_soon OR g.release_date >= :recentSince) DESC,
                     g.review_count DESC NULLS LAST,
                     g.id
            LIMIT :limit""")
    List<TranslationTarget> findKoreanTranslationTargets(@Param("recentSince") LocalDate recentSince,
                                                         @Param("limit") int limit);

    interface TranslationTarget {

        Long getGameId();

        String getShortDescription();
    }
}
