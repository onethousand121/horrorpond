package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.ingestion.client.AppDetailsResult;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * appdetails의 data 노드(JSON 문자열)를 도메인 입력으로 변환한다.
 * 필수값(type="game"인 경우 name) 외에는 누락돼도 null/빈 값으로 처리한다.
 */
@Component
public class SteamAppDetailsParser {

    static final String TYPE_GAME = "game";

    /**
     * Steam categories 중 협동 계열 (실제 응답으로 확인한 id, 설명은 l=koreana 기준):
     * 9 협동, 38 온라인 협동, 39 스크린 공유 및 분할 협동, 48 LAN 협동.
     * 44 Remote Play Together는 원격 플레이 기능이라 제외한다.
     */
    static final Set<Integer> COOP_CATEGORY_IDS = Set.of(9, 38, 39, 48);

    /**
     * Steam content_descriptors 중 성인 콘텐츠: 3 Adult Only Sexual Content, 4 Frequent Nudity or Sexual Content.
     * (1 Some Nudity, 2 Violence/Gore, 5 General Mature Content는 공포게임에 흔해서 성인으로 보지 않는다)
     */
    static final Set<Integer> ADULT_DESCRIPTOR_IDS = Set.of(3, 4);

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    // &nbsp;를 unescape하면  이 되는데 \s에 포함되지 않으므로 따로 넣는다
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");

    /**
     * l=koreana 응답은 한국어 형식이 기본이고, 영어 형식이 섞여 올 수 있어 순서대로 시도한다.
     */
    private static final List<DateTimeFormatter> RELEASE_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy'년' M'월' d'일'", Locale.KOREAN),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH));

    /** 이름이 없어 게임으로 만들 수 없는 앱 (normalize가 건너뛰고 반영 완료로 표시한다) */
    static final String TYPE_UNNAMED = "game-without-name";

    public ParsedSteamApp parse(String dataJson) {
        JsonNode root = JSON.readTree(dataJson);
        String type = text(root, "type");
        if (!TYPE_GAME.equals(type)) {
            return new ParsedSteamApp(type, null, List.of(), List.of(), null, SteamLanguages.NONE);
        }
        ParsedSteamApp.EnglishText english = english(root.get(AppDetailsResult.Found.ENGLISH));
        // 한국어 응답에 이름이 비어 있는 게임이 있다. 영어 이름으로 대신하고, 둘 다 없으면 게임으로 만들지 않는다
        String title = firstNonBlank(text(root, "name"), english == null ? null : english.title());
        if (title == null) {
            return new ParsedSteamApp(TYPE_UNNAMED, null, List.of(), List.of(), null, SteamLanguages.NONE);
        }
        JsonNode release = root.path("release_date");
        String releaseDateText = text(release, "date");
        SteamGameData data = new SteamGameData(
                title,
                cleanDescription(text(root, "short_description")),
                text(root, "header_image"),
                parseReleaseDate(releaseDateText),
                releaseDateText,
                release.path("coming_soon").asBoolean(false),
                isCoop(root),
                reviewCount(root),
                isAdult(root),
                media(root),
                List.of());
        return new ParsedSteamApp(type, data, names(root, "developers"), names(root, "publishers"), english,
                SteamLanguages.parse(text(root, "supported_languages")));
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private static ParsedSteamApp.EnglishText english(JsonNode english) {
        if (english == null || !english.isObject()) {
            return null;
        }
        return new ParsedSteamApp.EnglishText(text(english, "name"),
                cleanDescription(text(english, "short_description")), text(english, "release_date"));
    }

    static LocalDate parseReleaseDate(String text) {
        if (text == null) {
            return null;
        }
        for (DateTimeFormatter format : RELEASE_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // 다음 형식 시도
            }
        }
        return null;
    }

    static String cleanDescription(String html) {
        if (html == null) {
            return null;
        }
        String text = HTML_TAG.matcher(html).replaceAll(" ");
        text = HtmlUtils.htmlUnescape(text);
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        return text.isEmpty() ? null : text;
    }

    /**
     * 트레일러 → 스크린샷 순. 현재 appdetails의 movies에는 mp4/webm이 없고
     * HLS(hls_h264)/DASH 매니페스트만 있으므로 hls_h264를 쓰고, 없으면 건너뛴다.
     */
    private static List<SteamGameData.Media> media(JsonNode root) {
        List<SteamGameData.Media> media = new ArrayList<>();
        for (JsonNode movie : root.path("movies")) {
            String url = text(movie, "hls_h264");
            if (url != null) {
                media.add(new SteamGameData.Media(MediaType.TRAILER, url, text(movie, "thumbnail")));
            }
        }
        for (JsonNode screenshot : root.path("screenshots")) {
            String url = text(screenshot, "path_full");
            if (url != null) {
                media.add(new SteamGameData.Media(MediaType.SCREENSHOT, url, text(screenshot, "path_thumbnail")));
            }
        }
        return media;
    }

    private static Integer reviewCount(JsonNode root) {
        JsonNode total = root.path("recommendations").get("total");
        return total != null && total.isNumber() ? total.asInt() : null;
    }

    private static boolean isAdult(JsonNode root) {
        for (JsonNode id : root.path("content_descriptors").path("ids")) {
            if (id.isNumber() && ADULT_DESCRIPTOR_IDS.contains(id.asInt())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCoop(JsonNode root) {
        for (JsonNode category : root.path("categories")) {
            JsonNode id = category.get("id");
            if (id != null && id.isNumber() && COOP_CATEGORY_IDS.contains(id.asInt())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> names(JsonNode root, String field) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode node : root.path(field)) {
            if (node.isString() && !node.asString().isBlank()) {
                names.add(node.asString().strip());
            }
        }
        return List.copyOf(names);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            return null;
        }
        String text = value.asString().strip();
        return text.isEmpty() ? null : text;
    }
}
