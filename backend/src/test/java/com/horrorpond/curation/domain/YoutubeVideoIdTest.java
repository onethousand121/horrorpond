package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeVideoIdTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=42s",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/live/dQw4w9WgXcQ?feature=share",
            "  https://youtu.be/dQw4w9WgXcQ  ",
    })
    void extractsVideoId(String input) {
        assertThat(YoutubeVideoId.parse(input)).isEqualTo("dQw4w9WgXcQ");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "https://www.youtube.com/@ryujaeil",
            "https://vimeo.com/123456789",
            "https://www.youtube.com/watch?v=short",
            "https://evil.example/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
    })
    void rejectsNonVideoUrls(String input) {
        assertThatThrownBy(() -> YoutubeVideoId.parse(input)).isInstanceOf(DomainValidationException.class);
    }
}
