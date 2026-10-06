package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurationArticleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void sixHighlightsAreRejected() {
        List<String> six = List.of("1", "2", "3", "4", "5", "6");

        assertThatThrownBy(() -> draft(six)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void fiveHighlightsAreAllowed() {
        assertThat(draft(List.of("1", "2", "3", "4", "5")).getHighlights()).hasSize(5);
    }

    @Test
    void highlightLongerThan40CharsIsRejected() {
        assertThat(draft(List.of("a".repeat(40))).getHighlights()).hasSize(1);

        assertThatThrownBy(() -> draft(List.of("a".repeat(41))))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void blankHighlightIsRejected() {
        assertThatThrownBy(() -> draft(List.of("ok", "   ")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void editAlsoValidatesHighlights() {
        CurationArticle article = draft(List.of("ok"));

        assertThatThrownBy(() -> article.edit("t", "o", "b", List.of("a".repeat(41))))
                .isInstanceOf(DomainValidationException.class);
        assertThat(article.getHighlights()).containsExactly("ok");
    }

    @Test
    void publishWithoutHighlightsIsRejected() {
        CurationArticle article = draft(List.of());

        assertThatThrownBy(() -> article.publish(NOW)).isInstanceOf(DomainValidationException.class);
        assertThat(article.isPublished()).isFalse();
    }

    @Test
    void publishedArticleCannotDropAllHighlights() {
        CurationArticle article = draft(List.of("ok"));
        article.publish(NOW);

        assertThatThrownBy(() -> article.edit("t", "o", "b", List.of()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void publishRecordsFirstPublishedAtOnly() {
        CurationArticle article = draft(List.of("ok"));
        article.publish(NOW);
        article.publish(NOW.plusSeconds(60));

        assertThat(article.isPublished()).isTrue();
        assertThat(article.getPublishedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void markSponsoredRequiresDisclosure(String disclosure) {
        CurationArticle article = draft(List.of("ok"));

        assertThatThrownBy(() -> article.markSponsored(disclosure))
                .isInstanceOf(DomainValidationException.class);
        assertThat(article.isSponsored()).isFalse();
    }

    @Test
    void sponsoredCanBeMarkedAndCleared() {
        CurationArticle article = draft(List.of("ok"));

        article.markSponsored("Sponsored by publisher");
        assertThat(article.isSponsored()).isTrue();
        assertThat(article.getSponsorDisclosure()).isEqualTo("Sponsored by publisher");

        article.clearSponsored();
        assertThat(article.isSponsored()).isFalse();
        assertThat(article.getSponsorDisclosure()).isNull();
    }

    private static CurationArticle draft(List<String> highlights) {
        return CurationArticle.draft(1L, "title", "one liner", "body", highlights);
    }
}
