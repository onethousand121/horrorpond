package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class SteamAppDetailsParserTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SteamAppDetailsParser parser = new SteamAppDetailsParser();

    @Test
    void parsesKoreanReleaseDateFromFixture() {
        SteamGameData data = parser.parse(fixtureData(1262350)).data();

        assertThat(data.title()).isEqualTo("SIGNALIS");
        assertThat(data.releaseDate()).isEqualTo(LocalDate.of(2022, 10, 27));
        assertThat(data.releaseDateText()).isEqualTo("2022년 10월 27일");
        assertThat(data.comingSoon()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Oct 27, 2022|2022-10-27",
            "27 Oct, 2022|2022-10-27",
            "2020년 9월 18일|2020-09-18"
    })
    void parsesSupportedReleaseDateFormats(String text, LocalDate expected) {
        SteamGameData data = parser.parse(modified(739630, d -> ((ObjectNode) d.get("release_date")).put("date", text))).data();

        assertThat(data.releaseDate()).isEqualTo(expected);
        assertThat(data.releaseDateText()).isEqualTo(text);
    }

    @Test
    void unparseableReleaseDateKeepsTextOnly() {
        SteamGameData data = parser.parse(modified(739630, d -> ((ObjectNode) d.get("release_date"))
                .put("date", "2027년 4분기")
                .put("coming_soon", true))).data();

        assertThat(data.releaseDate()).isNull();
        assertThat(data.releaseDateText()).isEqualTo("2027년 4분기");
        assertThat(data.comingSoon()).isTrue();
    }

    @Test
    void shortDescriptionStripsTagsAndUnescapesEntities() {
        String html = "<p>Ghosts &amp; <b>hunters</b>&nbsp;&#8212; it&#39;s <br/>scary</p>";
        SteamGameData data = parser.parse(modified(739630, d -> d.put("short_description", html))).data();

        assertThat(data.shortDescription()).isEqualTo("Ghosts & hunters — it's scary");
    }

    @Test
    void plainShortDescriptionIsKeptAsIs() {
        SteamGameData data = parser.parse(fixtureData(594330)).data();

        assertThat(data.shortDescription()).startsWith("Visage는 1인칭 심리적 공포 게임입니다.");
    }

    @Test
    void screenshotsUsePathFullAndThumbnail() {
        SteamGameData data = parser.parse(fixtureData(739630)).data();

        assertThat(data.media()).filteredOn(m -> m.type() == MediaType.SCREENSHOT).hasSize(48)
                .first().satisfies(m -> {
                    assertThat(m.url()).contains("/ss_").contains(".1920x1080.jpg");
                    assertThat(m.thumbnailUrl()).contains(".600x338.jpg");
                });
    }

    @Test
    void trailersUseHlsManifestAndThumbnail() {
        SteamGameData data = parser.parse(fixtureData(739630)).data();

        assertThat(data.media()).filteredOn(m -> m.type() == MediaType.TRAILER).hasSize(7)
                .allSatisfy(m -> {
                    assertThat(m.url()).contains("hls_264_master.m3u8");
                    // 최근 트레일러는 movie_600x337.jpg, 오래된 트레일러는 movie.293x165.jpg
                    assertThat(m.thumbnailUrl()).containsPattern("/movie[._]");
                });
        assertThat(data.media().get(0).type()).isEqualTo(MediaType.TRAILER);
    }

    @Test
    void movieWithoutUsableUrlIsSkipped() {
        SteamGameData data = parser.parse(modified(739630, d -> {
            d.withArray("movies").forEach(movie -> ((ObjectNode) movie).remove("hls_h264"));
        })).data();

        assertThat(data.media()).noneMatch(m -> m.type() == MediaType.TRAILER);
    }

    @Test
    void reviewCountAndAdultFromFixture() {
        SteamGameData signalis = parser.parse(fixtureData(1262350)).data();
        assertThat(signalis.reviewCount()).isEqualTo(30987);
        // 2(폭력), 5(일반 성인용)는 성인으로 보지 않는다
        assertThat(signalis.adult()).isFalse();
    }

    @Test
    void adultDescriptorMarksAdultAndMissingRecommendationsIsNull() {
        SteamGameData data = parser.parse(modified(739630, d -> {
            d.remove("recommendations");
            ((ObjectNode) d.get("content_descriptors")).putArray("ids").add(1).add(4);
        })).data();
        assertThat(data.reviewCount()).isNull();
        assertThat(data.adult()).isTrue();
    }

    @Test
    void englishTextIsParsedWhenPresent() {
        assertThat(parser.parse(fixtureData(739630)).english()).isNull();

        String withEnglish = new AppDetailsResult.Found(fixtureData(739630)).withEnglish("""
                {"name":"Phasmophobia","short_description":"Ghost <b>hunting</b>","type":"game",
                "release_date":{"coming_soon":false,"date":"Sep 18, 2020"}}""").dataJson();

        ParsedSteamApp.EnglishText english = parser.parse(withEnglish).english();
        assertThat(english).isEqualTo(new ParsedSteamApp.EnglishText("Phasmophobia", "Ghost hunting", "Sep 18, 2020"));
        assertThat(parser.parse(withEnglish).data().title()).as("한국어 데이터는 그대로").isEqualTo("Phasmophobia");
    }

    @Test
    void coopFromFixtureCategories() {
        // 739630: 9 협동, 38 온라인 협동 / 1262350, 594330: 싱글 플레이어만
        assertThat(parser.parse(fixtureData(739630)).data().coop()).isTrue();
        assertThat(parser.parse(fixtureData(1262350)).data().coop()).isFalse();
        assertThat(parser.parse(fixtureData(594330)).data().coop()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"9,true", "38,true", "39,true", "48,true", "44,false", "1,false", "49,false"})
    void eachCoopCategoryIdAloneMarksCoop(int categoryId, boolean expected) {
        String json = modified(1262350, d -> {
            d.putArray("categories").addObject().put("id", categoryId).put("description", "x");
        });

        assertThat(parser.parse(json).data().coop()).isEqualTo(expected);
    }

    @Test
    void developersAndPublishersFromFixture() {
        ParsedSteamApp parsed = parser.parse(fixtureData(1262350));

        assertThat(parsed.developerNames()).containsExactly("rose-engine");
        assertThat(parsed.publisherNames()).containsExactly("Balor Games");
    }

    @Test
    void missingOptionalFieldsBecomeNull() {
        ParsedSteamApp parsed = parser.parse("{\"type\":\"game\",\"name\":\"Bare\"}");

        assertThat(parsed.isGame()).isTrue();
        SteamGameData data = parsed.data();
        assertThat(data.title()).isEqualTo("Bare");
        assertThat(data.shortDescription()).isNull();
        assertThat(data.headerImageUrl()).isNull();
        assertThat(data.releaseDate()).isNull();
        assertThat(data.releaseDateText()).isNull();
        assertThat(data.comingSoon()).isFalse();
        assertThat(data.coop()).isFalse();
        assertThat(data.media()).isEmpty();
        assertThat(parsed.developerNames()).isEmpty();
        assertThat(parsed.publisherNames()).isEmpty();
    }

    @Test
    void nonGameTypeHasNoGameData() {
        ParsedSteamApp parsed = parser.parse(fixtureData(5060920));

        assertThat(parsed.type()).isEqualTo("dlc");
        assertThat(parsed.isGame()).isFalse();
        assertThat(parsed.data()).isNull();
    }

    static String fixtureData(int appid) {
        return JSON.readTree(Fixtures.appDetails(appid)).get(String.valueOf(appid)).get("data").toString();
    }

    static String modified(int appid, Consumer<ObjectNode> change) {
        ObjectNode data = (ObjectNode) JSON.readTree(fixtureData(appid));
        change.accept(data);
        return data.toString();
    }

    @Test
    void blankKoreanNameFallsBackToEnglishOrSkipsTheApp() {
        ParsedSteamApp withEnglish = parser.parse("""
                {"type":"game","name":"","english":{"name":"Plague Town","short_description":"x"}}""");
        assertThat(withEnglish.isGame()).isTrue();
        assertThat(withEnglish.data().title()).isEqualTo("Plague Town");

        ParsedSteamApp nameless = parser.parse("""
                {"type":"game","name":" "}""");
        assertThat(nameless.isGame()).isFalse();
        assertThat(nameless.data()).isNull();
    }

    @Test
    void supportedLanguagesBecomeCodesWithAudioMarked() {
        ParsedSteamApp parsed = parser.parse(fixtureData(5060920).replace("\"type\":\"dlc\"", "\"type\":\"game\""));
        assertThat(parsed.languages().languages())
                .containsExactly("en", "fr", "it", "de", "es", "ru", "zh-Hans", "pt-BR", "th", "zh-Hant", "ja", "ko",
                        "pl", "es-419", "tr");
        assertThat(parsed.languages().audioLanguages()).containsExactly("en", "fr", "ja");
    }

    @Test
    void englishLanguageNamesAndUnknownNames() {
        SteamLanguages parsed = SteamLanguages.parse(
                "English<strong>*</strong>, Korean, Klingon<br><strong>*</strong>languages with full audio support");
        assertThat(parsed.languages()).containsExactly("en", "ko");
        assertThat(parsed.audioLanguages()).containsExactly("en");
        assertThat(SteamLanguages.parse(null).languages()).isEmpty();
    }
}
