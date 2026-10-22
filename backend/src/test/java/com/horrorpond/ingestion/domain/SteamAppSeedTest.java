package com.horrorpond.ingestion.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SteamAppSeedTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    void requestManuallyClearsNotHorrorVerdict() {
        SteamAppSeed seed = SteamAppSeed.discovered(1, DiscoveredBy.STEAMSPY_TAG, NOW);
        seed.recordHorrorTag(HorrorTag.NOT_HORROR, NOW);

        seed.requestManually();

        assertThat(seed.getDiscoveredBy()).isEqualTo(DiscoveredBy.MANUAL);
        assertThat(seed.getFetchStatus()).isEqualTo(FetchStatus.PENDING);
        assertThat(seed.getHorrorTag()).isEqualTo(HorrorTag.UNCHECKED);
        assertThat(seed.getHorrorTagCheckedAt()).isNull();
        assertThat(seed.needsHorrorTagCheck()).isFalse();
    }

    @Test
    void emptySpyTagsAreRefetchedOnlyAfterAWeek() {
        SteamAppSeed seed = SteamAppSeed.discovered(1, DiscoveredBy.MANUAL, NOW);
        assertThat(seed.needsSpyTags(NOW)).isTrue();

        seed.recordSpyTags(List.of());
        seed.markFetched(FetchStatus.OK, NOW);

        assertThat(seed.needsSpyTags(NOW.plus(Duration.ofDays(6)))).isFalse();
        assertThat(seed.needsSpyTags(NOW.plus(Duration.ofDays(7)))).isTrue();
    }

    @Test
    void nonEmptySpyTagsAreNotRefetched() {
        SteamAppSeed seed = SteamAppSeed.discovered(1, DiscoveredBy.MANUAL, NOW);
        seed.recordSpyTags(List.of("Horror"));
        seed.markFetched(FetchStatus.OK, NOW);

        assertThat(seed.needsSpyTags(NOW.plus(Duration.ofDays(30)))).isFalse();
    }

    @Test
    void recordSpyTagsReportsWhetherTagsChanged() {
        SteamAppSeed seed = SteamAppSeed.discovered(1, DiscoveredBy.MANUAL, NOW);

        assertThat(seed.recordSpyTags(List.of())).isTrue();
        assertThat(seed.recordSpyTags(List.of())).isFalse();
        assertThat(seed.recordSpyTags(List.of("Horror"))).isTrue();
        assertThat(seed.recordSpyTags(List.of("Horror"))).isFalse();
    }
}
