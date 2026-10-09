package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.MediaType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItchPageParserTest {

    private final ItchPageParser parser = new ItchPageParser();

    @Test
    void parsesListingCellsWithRatingCounts() {
        String content = String.join("",
                ItchFixtures.listingCell(1, "https://a.itch.io/one", "One &amp; Only", 11241),
                ItchFixtures.listingCell(2, "https://b.itch.io/two", "Two", null),
                ItchFixtures.listingCell(1, "https://a.itch.io/one", "One &amp; Only", 11241),
                ItchFixtures.listingCell(3, "https://example.com/elsewhere", "Not itch", 900));

        assertThat(parser.parseListing(content)).containsExactly(
                new ItchListing(1, "https://a.itch.io/one", "One & Only", 11241),
                new ItchListing(2, "https://b.itch.io/two", "Two", null));
    }

    @Test
    void emptyListing() {
        assertThat(parser.parseListing("")).isEmpty();
        assertThat(parser.parseListing(null)).isEmpty();
    }

    @Test
    void parsesGamePage() {
        ParsedItchGame game = parser.parseGame(ItchFixtures.gamePage(42, "Night Shift", "Clock in. Don't look back.", 1234));

        assertThat(game.itchId()).isEqualTo(42);
        assertThat(game.title()).isEqualTo("Night Shift");
        assertThat(game.description()).isEqualTo("Clock in. Don't look back.");
        assertThat(game.coverUrl()).isEqualTo("https://img.itch.zone/cover/original/c.png");
        assertThat(game.ratingCount()).isEqualTo(1234);
        // 마지막 갱신일(Updated)이 아니라 처음 공개한 날
        assertThat(game.releaseDate()).isEqualTo(LocalDate.of(2024, 3, 3));
        // 장르가 먼저, 태그는 Steam 태그와 같은 이름이면 장르 자동 분류에 그대로 쓰인다
        assertThat(game.tags()).containsExactly("Adventure", "Horror", "Psychological Horror");
        assertThat(game.languages()).containsExactly("en", "ko", "pt-BR");
        assertThat(game.authors()).containsExactly("Dev Studio");
        // 속성 순서가 달라도, src 없이 srcset만 있어도 읽는다
        assertThat(game.screenshots()).extracting("type", "url", "thumbnailUrl").containsExactly(
                org.assertj.core.groups.Tuple.tuple(MediaType.SCREENSHOT, "https://img.itch.zone/s1/original/a.png",
                        "https://img.itch.zone/s1/347x500/a.png"),
                org.assertj.core.groups.Tuple.tuple(MediaType.SCREENSHOT, "https://img.itch.zone/s2/original/b.png",
                        "https://img.itch.zone/s2/347x500/b.png"));
    }

    @Test
    void gameWithoutRatingsOrPublishedDate() {
        String html = ItchFixtures.gamePage(7, "Tiny", "Short.", null)
                .replaceAll("<tr><td>Published</td>.*?</tr>", "");

        ParsedItchGame game = parser.parseGame(html);

        assertThat(game.ratingCount()).isNull();
        assertThat(game.releaseDate()).isNull();
    }

    @Test
    void rejectsPagesThatAreNotGames() {
        assertThatThrownBy(() -> parser.parseGame("<html><head><meta content=\"users/5\" name=\"itch:path\"/></head></html>"))
                .isInstanceOf(ItchPageParseException.class);
        assertThatThrownBy(() -> parser.parseGame("<html></html>")).isInstanceOf(ItchPageParseException.class);
    }

    @Test
    void dateTexts() {
        LocalDate date = LocalDate.of(2026, 9, 16);
        assertThat(ItchPageParser.koreanDateText(date)).isEqualTo("2026년 9월 16일");
        assertThat(ItchPageParser.englishDateText(date)).isEqualTo("Sep 16, 2026");
        assertThat(ItchPageParser.koreanDateText(null)).isNull();
    }
}
