package com.horrorpond.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Outlast|outlast",
            "Half-Life: Alyx|half-life-alyx",
            "Five Nights at Freddy's|five-nights-at-freddys",
            "  Amnesia   The Dark Descent  |amnesia-the-dark-descent",
            "Café Ötzi|cafe-otzi",
            "SIGNALIS 2|signalis-2",
            "공포 Horror 게임|horror"
    })
    void slugify(String title, String expected) {
        assertThat(SlugGenerator.slugify(title)).isEqualTo(expected);
    }

    @Test
    void fallsBackToGameWhenNothingRemains() {
        assertThat(SlugGenerator.slugify("공포게임")).isEqualTo("game");
        assertThat(SlugGenerator.slugify("!!!")).isEqualTo("game");
        assertThat(SlugGenerator.slugify(null)).isEqualTo("game");
    }

    @Test
    void steamCandidateAppendsAppid() {
        assertThat(SlugGenerator.forSteamCandidate("Outlast", 238320)).isEqualTo("outlast-238320");
        assertThat(SlugGenerator.forSteamCandidate("공포게임", 42)).isEqualTo("game-42");
    }

    @Test
    void truncatesLongTitles() {
        String slug = SlugGenerator.slugify("a ".repeat(300));
        assertThat(slug.length()).isLessThanOrEqualTo(SlugGenerator.MAX_LENGTH);
        assertThat(slug).doesNotEndWith("-");
    }
}
