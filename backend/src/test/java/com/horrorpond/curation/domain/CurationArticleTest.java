package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.domain.Language;
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

    @Test
    void translationIsShownOnlyInEnglishAndOnlyWhenPresent() {
        CurationArticle article = draft(List.of("무섭다"));

        assertThat(article.title(Language.EN)).isEqualTo("title");
        assertThat(article.isShownIn(Language.EN)).isFalse();

        article.translate(" English title ", "English one liner", "English body", List.of("Scary"));

        assertThat(article.title(Language.EN)).isEqualTo("English title");
        assertThat(article.oneLiner(Language.EN)).isEqualTo("English one liner");
        assertThat(article.body(Language.EN)).isEqualTo("English body");
        assertThat(article.highlights(Language.EN)).containsExactly("Scary");
        assertThat(article.title(Language.KO)).isEqualTo("title");
        assertThat(article.highlights(Language.KO)).containsExactly("무섭다");
    }

    @Test
    void translationFollowsArticleRules() {
        CurationArticle article = draft(List.of("ok"));

        assertThatThrownBy(() -> article.translate("t", " ", "body", List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> article.translate("t", "o", "b", List.of("1", "2", "3", "4", "5", "6")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(article.hasTranslation()).isFalse();
    }

    @Test
    void editingKoreanMakesTranslationStaleUntilRetranslated() {
        CurationArticle article = draft(List.of("ok"));
        article.translate("t", "o", "b", null);
        assertThat(article.isTranslationStale()).isFalse();
        assertThat(article.getHighlightsEn()).isEmpty();

        article.edit("title", "one liner", "body", List.of("ok"));
        assertThat(article.isTranslationStale()).isFalse();

        article.edit("title", "one liner", "new body", List.of("ok"));
        assertThat(article.isTranslationStale()).isTrue();

        article.translate("t", "o", "b2", null);
        assertThat(article.isTranslationStale()).isFalse();

        article.clearTranslation();
        assertThat(article.hasTranslation()).isFalse();
        assertThat(article.isTranslationStale()).isFalse();
        assertThat(article.body(Language.EN)).isEqualTo("new body");
    }

    private static CurationArticle draft(List<String> highlights) {
        return CurationArticle.draft(1L, "title", "one liner", "body", highlights);
    }
}
