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
        game.publish(true, T1);

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

        game.applySteamData(new SteamGameData("Title", null, null, null, null, true, false,
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
    void publishWithoutPublishedArticleFails() {
        Game game = Game.manual("Manual", "manual");

        assertThatThrownBy(() -> game.publish(false, T1))
                .isInstanceOf(DomainStateException.class);
        assertThat(game.getStatus()).isEqualTo(GameStatus.CANDIDATE);
        assertThat(game.getPublishedAt()).isNull();
    }

    @Test
    void republishKeepsFirstPublishedAt() {
        Game game = Game.manual("Manual", "manual");
        game.publish(true, T1);
        game.hide();

        game.publish(true, T2);

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
                LocalDate.of(2013, 9, 4), "4 Sep, 2013", false, true,
                List.of(new SteamGameData.Media(MediaType.SCREENSHOT, "https://s/1", "https://t/1"),
                        new SteamGameData.Media(MediaType.TRAILER, "https://v/1", null)),
                List.of(new SteamGameData.Credit(dev, DeveloperRole.DEVELOPER)));
    }
}
