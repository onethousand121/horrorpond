package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KoreanPatchTest {

    @Test
    void keepsHttpsUrlAndTrimsOptionalText() {
        KoreanPatch patch = KoreanPatch.create(1L, "  https://cafe.example.com/patch?id=1  ", " 한글팀 ", " ", 0);

        assertThat(patch.getUrl()).isEqualTo("https://cafe.example.com/patch?id=1");
        assertThat(patch.getAuthor()).isEqualTo("한글팀");
        assertThat(patch.getNote()).isNull();
    }

    @Test
    void acceptsUppercaseScheme() {
        assertThat(KoreanPatch.create(1L, "HTTPS://Example.com/p", null, null, 0).getUrl())
                .isEqualTo("HTTPS://Example.com/p");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "http://example.com/patch",
            "javascript:alert(1)",
            "data:text/html,hi",
            "//example.com/patch",
            "/patch",
            "example.com/patch",
            "https:///patch",
            "https://user:pw@example.com/patch",
            "https://exa mple.com/patch",
    })
    void rejectsNonHttpsOrHostlessUrls(String url) {
        assertThatThrownBy(() -> KoreanPatch.create(1L, url, null, null, 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsTooLongValues() {
        String longUrl = "https://example.com/" + "a".repeat(KoreanPatch.MAX_URL_LENGTH);
        assertThatThrownBy(() -> KoreanPatch.create(1L, longUrl, null, null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> KoreanPatch.create(1L, "https://example.com", "a".repeat(101), null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> KoreanPatch.create(1L, "https://example.com", null, "a".repeat(201), 0))
                .isInstanceOf(DomainValidationException.class);
    }
}
