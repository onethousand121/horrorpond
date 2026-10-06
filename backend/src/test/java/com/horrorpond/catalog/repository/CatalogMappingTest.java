package com.horrorpond.catalog.repository;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameDeveloper;
import com.horrorpond.catalog.domain.GameMedia;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.support.JpaSliceTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@JpaSliceTest
class CatalogMappingTest {

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GenreRepository genreRepository;

    @Autowired
    DeveloperRepository developerRepository;

    @Autowired
    EntityManager em;

    @Test
    void genreRoundTrip() {
        genreRepository.save(Genre.create("Survival", "survival", null, 2));
        genreRepository.save(Genre.create("Psychological", "psychological", "mind games", 1));
        flushAndClear();

        Genre found = genreRepository.findBySlug("psychological").orElseThrow();
        assertThat(found.getName()).isEqualTo("Psychological");
        assertThat(found.getDescription()).isEqualTo("mind games");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(genreRepository.findAllByOrderByDisplayOrderAsc())
                .extracting(Genre::getSlug).containsExactly("psychological", "survival");
    }

    @Test
    void developerRoundTrip() {
        Developer developer = Developer.create("Red Barrels", "red-barrels");
        developer.changeWebsite("https://redbarrelsgames.com");
        developerRepository.save(developer);
        flushAndClear();

        Developer found = developerRepository.findByName("Red Barrels").orElseThrow();
        assertThat(found.getSlug()).isEqualTo("red-barrels");
        assertThat(found.getWebsiteUrl()).isEqualTo("https://redbarrelsgames.com");
    }

    @Test
    void gameAggregateRoundTrip() {
        Genre genre = genreRepository.save(Genre.create("Psychological", "psychological", null, 0));
        Developer dev = developerRepository.save(Developer.create("Red Barrels", "red-barrels"));

        Game game = Game.candidateFromSteam(238320, "Outlast", "outlast-238320");
        game.replaceGenres(Set.of(genre));
        game.applySteamData(new SteamGameData("Outlast", "Hell is an experiment you can't survive.",
                "https://img/header.jpg", LocalDate.of(2013, 9, 4), "4 Sep, 2013", false, false,
                List.of(new SteamGameData.Media(MediaType.SCREENSHOT, "https://s/1", "https://t/1"),
                        new SteamGameData.Media(MediaType.TRAILER, "https://v/1", null)),
                List.of(new SteamGameData.Credit(dev, DeveloperRole.DEVELOPER),
                        new SteamGameData.Credit(dev, DeveloperRole.PUBLISHER))));
        gameRepository.save(game);
        flushAndClear();

        Game found = gameRepository.findBySourceAndExternalId(GameSource.STEAM, "238320").orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Outlast");
        assertThat(found.getSlug()).isEqualTo("outlast-238320");
        assertThat(found.getStatus()).isEqualTo(GameStatus.CANDIDATE);
        assertThat(found.getReleaseDate()).isEqualTo(LocalDate.of(2013, 9, 4));
        assertThat(found.isComingSoon()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getMedia())
                .extracting(GameMedia::getType, GameMedia::getUrl, GameMedia::getSortOrder)
                .containsExactly(tuple(MediaType.SCREENSHOT, "https://s/1", 0),
                        tuple(MediaType.TRAILER, "https://v/1", 1));
        assertThat(found.getStoreLinks()).singleElement().satisfies(link -> {
            assertThat(link.getStore()).isEqualTo(Store.STEAM);
            assertThat(link.getUrl()).isEqualTo("https://store.steampowered.com/app/238320");
        });
        assertThat(found.getGenres()).extracting(Genre::getSlug).containsExactly("psychological");
        assertThat(found.getDevelopers())
                .extracting(gd -> gd.getDeveloper().getId(), GameDeveloper::getRole)
                .containsExactlyInAnyOrder(tuple(dev.getId(), DeveloperRole.DEVELOPER),
                        tuple(dev.getId(), DeveloperRole.PUBLISHER));
    }

    @Test
    void reapplyingSteamDataWithSameCreditDoesNotViolatePrimaryKey() {
        Developer dev = developerRepository.save(Developer.create("Dev", "dev"));
        Developer publisher = developerRepository.save(Developer.create("Pub", "pub"));
        Game game = Game.candidateFromSteam(1, "Title", "title-1");
        game.applySteamData(steamData(List.of(new SteamGameData.Credit(dev, DeveloperRole.DEVELOPER))));
        gameRepository.save(game);
        flushAndClear();

        Game loaded = gameRepository.findBySourceAndExternalId(GameSource.STEAM, "1").orElseThrow();
        Developer reloadedDev = developerRepository.findByName("Dev").orElseThrow();
        Developer reloadedPub = developerRepository.findByName("Pub").orElseThrow();
        loaded.applySteamData(steamData(List.of(
                new SteamGameData.Credit(reloadedDev, DeveloperRole.DEVELOPER),
                new SteamGameData.Credit(reloadedPub, DeveloperRole.PUBLISHER))));
        flushAndClear();

        Game found = gameRepository.findById(loaded.getId()).orElseThrow();
        assertThat(found.getDevelopers())
                .extracting(gd -> gd.getDeveloper().getId(), GameDeveloper::getRole)
                .containsExactlyInAnyOrder(tuple(dev.getId(), DeveloperRole.DEVELOPER),
                        tuple(publisher.getId(), DeveloperRole.PUBLISHER));
        assertThat(found.getMedia()).hasSize(1);
        assertThat(found.getStoreLinks()).hasSize(1);
    }

    @Test
    void manualGameHasNoExternalId() {
        Game saved = gameRepository.save(Game.manual("Indie Horror", "indie-horror"));
        flushAndClear();

        Game found = gameRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSource()).isEqualTo(GameSource.MANUAL);
        assertThat(found.getExternalId()).isNull();
    }

    private static SteamGameData steamData(List<SteamGameData.Credit> credits) {
        return new SteamGameData("Title", null, null, null, "Coming soon", true, false,
                List.of(new SteamGameData.Media(MediaType.SCREENSHOT, "https://s/1", null)),
                credits);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
