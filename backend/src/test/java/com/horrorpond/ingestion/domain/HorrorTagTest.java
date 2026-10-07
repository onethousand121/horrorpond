package com.horrorpond.ingestion.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HorrorTagTest {

    @Test
    void horrorInTopTenTagsIsHorror() {
        // L4D2는 Horror가 9위
        assertThat(HorrorTag.classify(List.of("Zombies", "Co-op", "FPS", "Multiplayer", "Shooter", "Action",
                "Online Co-Op", "Team-Based", "Horror"))).isEqualTo(HorrorTag.HORROR);
    }

    @Test
    void horrorBelowTopTenIsNotHorror() {
        // Undertale: Psychological Horror 18위, Horror 20위
        assertThat(HorrorTag.classify(List.of("Great Soundtrack", "Story Rich", "Choices Matter", "Multiple Endings",
                "Pixel Graphics", "Funny", "RPG", "Singleplayer", "Indie", "2D", "Comedy", "Replay Value",
                "Bullet Hell", "Cute", "Memes", "Retro", "Dark", "Psychological Horror", "Dating Sim", "Horror")))
                .isEqualTo(HorrorTag.NOT_HORROR);
    }

    @Test
    void horrorSubgenreTagIsHorror() {
        assertThat(HorrorTag.classify(List.of("Atmospheric", "Psychological Horror"))).isEqualTo(HorrorTag.HORROR);
    }

    @Test
    void topTagsWithoutHorrorIsNotHorror() {
        // PUBG: Horror 태그 목록에는 있지만 상위 20개 태그에는 없다
        assertThat(HorrorTag.classify(List.of("Survival", "Shooter", "Battle Royale")))
                .isEqualTo(HorrorTag.NOT_HORROR);
    }

    @Test
    void noTagDataIsHorror() {
        assertThat(HorrorTag.classify(List.of())).isEqualTo(HorrorTag.HORROR);
    }
}
