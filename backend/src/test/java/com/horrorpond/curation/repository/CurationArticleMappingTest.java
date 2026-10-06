package com.horrorpond.curation.repository;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.support.JpaSliceTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaSliceTest
class CurationArticleMappingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    CurationArticleRepository articleRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    EntityManager em;

    @Test
    void articleRoundTripWithHighlightsArray() {
        Long gameId = gameRepository.save(Game.manual("Outlast", "outlast")).getId();
        CurationArticle article = CurationArticle.draft(gameId, "Title", "One liner", "Body",
                List.of("Found footage", "No weapons", "Asylum"));
        article.markSponsored("Key provided by publisher");
        article.publish(NOW);
        articleRepository.save(article);
        em.flush();
        em.clear();

        CurationArticle found = articleRepository.findByGameId(gameId).orElseThrow();
        assertThat(found.getHighlights()).containsExactly("Found footage", "No weapons", "Asylum");
        assertThat(found.isSponsored()).isTrue();
        assertThat(found.getSponsorDisclosure()).isEqualTo("Key provided by publisher");
        assertThat(found.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(found.getPublishedAt()).isEqualTo(NOW);
        assertThat(found.getCreatedAt()).isNotNull();

        Object[] row = (Object[]) em.createNativeQuery(
                        "select cardinality(highlights), highlights[2] from curation_article where game_id = :id")
                .setParameter("id", gameId)
                .getSingleResult();
        assertThat(((Number) row[0]).intValue()).isEqualTo(3);
        assertThat(row[1]).isEqualTo("No weapons");
    }
}
