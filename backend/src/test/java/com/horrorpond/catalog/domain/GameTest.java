package com.horrorpond.catalog.domain;

import com.horrorpond.common.domain.DomainStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void applySteamDataDoesNotChangeStatusSlugOrGenres() {
        Game game = Game.candidateFromSteam(238320, "Old Title", "outlast-238320");
        Genre genre = Genre.create("Psychological", "psychological", null, 0);
        game.replaceGenres(Set.of(genre));
        game.changeSlug("outlast");
        game.publish(T1);

        Developer dev = Developer.create("Red Barrels", "red-barrels");
        game.applySteamData(steamData("Outlast", dev));

        assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(game.getPublishedAt()).isEqualTo(T1);
        assertThat(game.getSlug()).isEqualTo("outlast");
        assertThat(game.getGenres()).containsExactly(genre);

        assertThat(game.getTitle()).isEqualTo("Outlast");
        assertThat(game.isCoop()).isTrue();
        assertThat(game.getReleaseDate()).isEqualTo(LocalDate.of(2013, 9, 4));
        assertThat(game.getMedia()).extracting(GameMedia::getSortOrder).containsExactly(0, 1);
        assertThat(game.getDevelopers()).extracting(GameDeveloper::getRole)
                .containsExactly(DeveloperRole.DEVELOPER);
        assertThat(game.getStoreLinks()).singleElement()
                .satisfies(link -> {
                    assertThat(link.getStore()).isEqualTo(Store.STEAM);
                    assertThat(link.getUrl()).isEqualTo("https://store.steampowered.com/app/238320");
                });
    }

    @Test
    void applySteamDataKeepsHiddenStatus() {
        Game game = Game.candidateFromSteam(1, "Title", "title-1");
        game.hide();

        game.applySteamData(steamData("New", Developer.create("Dev", "dev")));

        assertThat(game.getStatus()).isEqualTo(GameStatus.HIDDEN);
    }

    @Test
    void reapplyingSteamDataReplacesMediaAndKeepsMatchingCredits() {
        Game game = Game.candidateFromSteam(1, "Title", "title-1");
        Developer dev = Developer.create("Dev", "dev");
        Developer publisher = Developer.create("Pub", "pub");
        game.applySteamData(steamData("Title", dev));
        GameDeveloper originalCredit = game.getDevelopers().get(0);

        game.applySteamData(new SteamGameData("Title", null, null, null, null, true, false, null, false,
                List.of(new SteamGameData.Media(MediaType.TRAILER, "https://v/2", null)),
                List.of(new SteamGameData.Credit(dev, DeveloperRole.DEVELOPER),
                        new SteamGameData.Credit(publisher, DeveloperRole.PUBLISHER))));

        assertThat(game.getMedia()).singleElement()
                .extracting(GameMedia::getUrl).isEqualTo("https://v/2");
        assertThat(game.getDevelopers()).hasSize(2).first().isSameAs(originalCredit);
        assertThat(game.getStoreLinks()).hasSize(1);
    }

    @Test
    void applySteamDataRejectsNonSteamGame() {
        Game game = Game.manual("Manual", "manual");

        assertThatThrownBy(() -> game.applySteamData(steamData("X", Developer.create("Dev", "dev"))))
                .isInstanceOf(DomainStateException.class);
    }

    @Test
    void changeCoopIsForNonSteamGamesOnly() {
        Game manual = Game.manual("Manual", "manual");
        manual.changeCoop(true);
        assertThat(manual.isCoop()).isTrue();

        Game steam = Game.candidateFromSteam(1, "Title", "title-1");
        assertThatThrownBy(() -> steam.changeCoop(true)).isInstanceOf(DomainStateException.class);
        assertThat(steam.isCoop()).isFalse();
    }

    @Test
    void candidatesAreAutoExposedWhenNotAdultAndUpcomingOrReviewed() {
        // (상태, 성인, 출시예정, 리뷰 수) → 노출 여부, 기준 리뷰 10
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, false, false, 10, 10)).isTrue();
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, false, false, 9, 10)).isFalse();
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, false, false, null, 10)).isFalse();
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, false, true, null, 10)).as("출시 예정").isTrue();
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, true, false, 99999, 10)).as("성인").isFalse();
        assertThat(Game.isPubliclyVisible(GameStatus.CANDIDATE, true, true, null, 10)).as("성인 출시 예정").isFalse();
    }

    @Test
    void publishedIsAlwaysVisibleAndHiddenNever() {
        assertThat(Game.isPubliclyVisible(GameStatus.PUBLISHED, true, false, null, 10)).isTrue();
        assertThat(Game.isPubliclyVisible(GameStatus.HIDDEN, false, true, 99999, 10)).isFalse();
    }

    @Test
    void applySteamDataSetsReviewCountAndAdult() {
        Game game = Game.candidateFromSteam(1, "Title", "title-1");
        game.applySteamData(new SteamGameData("Title", null, null, null, null, false, false, 42, true, List.of(),
                List.of()));
        game.applySteamTags(List.of("Horror", "Psychological Horror"));

        assertThat(game.getReviewCount()).isEqualTo(42);
        assertThat(game.isAdult()).isTrue();
        assertThat(game.getTags()).containsExactly("Horror", "Psychological Horror");
        assertThat(game.isPubliclyVisible(10)).isFalse();
    }

    @Test
    void republishKeepsFirstPublishedAt() {
        Game game = Game.manual("Manual", "manual");
        game.publish(T1);
        game.hide();

        game.publish(T2);

        assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(game.getPublishedAt()).isEqualTo(T1);
    }

    @Test
    void unhideReturnsToCandidate() {
        Game game = Game.manual("Manual", "manual");
        assertThatThrownBy(game::unhide).isInstanceOf(DomainStateException.class);

        game.hide();
        game.unhide();

        assertThat(game.getStatus()).isEqualTo(GameStatus.CANDIDATE);
    }

    private static SteamGameData steamData(String title, Developer dev) {
        return new SteamGameData(title, "desc", "https://img/header.jpg",
                LocalDate.of(2013, 9, 4), "4 Sep, 2013", false, true, 1234, false,
                List.of(new SteamGameData.Media(MediaType.SCREENSHOT, "https://s/1", "https://t/1"),
                        new SteamGameData.Media(MediaType.TRAILER, "https://v/1", null)),
                List.of(new SteamGameData.Credit(dev, DeveloperRole.DEVELOPER)));
    }
}
