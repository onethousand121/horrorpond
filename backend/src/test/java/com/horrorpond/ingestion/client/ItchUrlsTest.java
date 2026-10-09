package com.horrorpond.ingestion.client;

import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItchUrlsTest {

    @Test
    void normalizesGameUrls() {
        assertThat(ItchUrls.normalize("https://Garula.itch.io/The-Freak-Circus/")).isEqualTo(
                "https://garula.itch.io/the-freak-circus");
        assertThat(ItchUrls.normalize("  http://mrdrnose.itch.io/votv?ref=x#top ")).isEqualTo(
                "https://mrdrnose.itch.io/votv");
        assertThat(ItchUrls.normalize("dev_name.itch.io/my_game")).isEqualTo("https://dev_name.itch.io/my_game");
    }

    /** 서버가 대신 요청을 보내므로 itch.io 게임 주소 모양이 아니면 받지 않는다 */
    @ParameterizedTest
    @ValueSource(strings = {
            "https://itch.io/games/tag-horror",
            "https://garula.itch.io",
            "https://garula.itch.io/game/devlog",
            "https://evil.com/x.itch.io/game",
            "https://garula.itch.io.evil.com/game",
            "https://user:pw@garula.itch.io/game",
            "ftp://garula.itch.io/game",
            ""})
    void rejectsOtherUrls(String url) {
        assertThatThrownBy(() -> ItchUrls.normalize(url)).isInstanceOf(DomainValidationException.class);
        assertThat(ItchUrls.isGameUrl(url)).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> ItchUrls.normalize(null)).isInstanceOf(DomainValidationException.class);
    }
}
