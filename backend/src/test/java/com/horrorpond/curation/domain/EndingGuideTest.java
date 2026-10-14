package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndingGuideTest {

    @Test
    void parsesShortsUrlAndKeepsLineBreaks() {
        EndingGuide ending = EndingGuide.create(1L, " 진엔딩 ", "열쇠 3개\n지하실로", "https://youtube.com/shorts/dQw4w9WgXcQ", 2);

        assertThat(ending.getName()).isEqualTo("진엔딩");
        assertThat(ending.getDescription()).isEqualTo("열쇠 3개\n지하실로");
        assertThat(ending.getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(ending.getSortOrder()).isEqualTo(2);
    }

    @Test
    void descriptionAndVideoAreOptional() {
        EndingGuide ending = EndingGuide.create(1L, "배드 엔딩", " ", "", 0);

        assertThat(ending.getDescription()).isNull();
        assertThat(ending.getYoutubeId()).isNull();
    }

    @Test
    void rejectsBlankOrTooLongValues() {
        assertThatThrownBy(() -> EndingGuide.create(1L, " ", null, null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> EndingGuide.create(1L, "a".repeat(101), null, null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> EndingGuide.create(1L, "a", "b".repeat(2001), null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> EndingGuide.create(1L, "a", null, "https://vimeo.com/1", 0))
                .isInstanceOf(DomainValidationException.class);
    }
}
