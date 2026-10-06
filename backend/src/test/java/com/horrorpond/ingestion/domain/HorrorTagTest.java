package com.horrorpond.ingestion.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HorrorTagTest {

    @Test
    void horrorAnywhereInTopTagsIsHorror() {
        // L4D2는 Horror가 9위
        assertThat(HorrorTag.classify(List.of("Zombies", "Co-op", "FPS", "Multiplayer", "Shooter", "Action",
                "Online Co-Op", "Team-Based", "Horror"))).isEqualTo(HorrorTag.HORROR);
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
