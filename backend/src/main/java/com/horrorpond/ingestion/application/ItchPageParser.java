package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.ingestion.client.ItchUrls;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * itch.io 목록 칸과 게임 페이지 HTML을 읽는다. 공식 API가 아니라 페이지 모양에 기대므로,
 * 꼭 필요한 값(게임 id, 이름)만 없으면 실패로 보고 나머지는 없으면 비워 둔다.
 */
@Component
public class ItchPageParser {

    static final int MAX_SCREENSHOTS = 10;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // ===== 목록 =====
    private static final Pattern CELL_START = Pattern.compile("<div data-game_id=\"(\\d+)\"");
    private static final Pattern CELL_TITLE = Pattern.compile(
            "<a [^>]*class=\"title game_link\"[^>]*href=\"([^\"]+)\"[^>]*>([^<]*)</a>");
    private static final Pattern CELL_RATING_COUNT = Pattern.compile("class=\"rating_count\">\\(([\\d,]+)");

    // ===== 게임 페이지 =====
    private static final Pattern META = Pattern.compile("<meta\\s[^>]*>");
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z:_-]+)=\"([^\"]*)\"");
    private static final Pattern LD_JSON = Pattern.compile(
            "<script type=\"application/ld\\+json\">(.*?)</script>", Pattern.DOTALL);
    private static final Pattern INFO_ROW = Pattern.compile("<tr><td>([^<]+)</td><td>(.*?)</td></tr>", Pattern.DOTALL);
    private static final Pattern LINK_TEXT = Pattern.compile("<a [^>]*>([^<]+)</a>");
    private static final Pattern LANGUAGE_LINK = Pattern.compile("href=\"[^\"]*/games/lang-([a-z0-9-]+)\"");
    private static final Pattern ABBR_TITLE = Pattern.compile("<abbr title=\"([^\"]+)\"");
    private static final Pattern SCREENSHOT = Pattern.compile("<a (?=[^>]*data-image_lightbox)([^>]*)>\\s*<img ([^>]*)>");
    private static final DateTimeFormatter ITCH_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter EN_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** itch.io 언어 주소(lang-xx) → 사이트 언어 코드 (SteamLanguages와 같은 코드) */
    private static final Map<String, String> LANGUAGES = Map.ofEntries(
            Map.entry("ko", "ko"), Map.entry("en", "en"), Map.entry("ja", "ja"),
            Map.entry("zh", "zh-Hans"), Map.entry("zh-cn", "zh-Hans"), Map.entry("zh-hans", "zh-Hans"),
            Map.entry("zh-tw", "zh-Hant"), Map.entry("zh-hk", "zh-Hant"), Map.entry("zh-hant", "zh-Hant"),
            Map.entry("fr", "fr"), Map.entry("de", "de"), Map.entry("it", "it"),
            Map.entry("es", "es"), Map.entry("es-es", "es"), Map.entry("es-419", "es-419"), Map.entry("es-mx", "es-419"),
            Map.entry("pt-br", "pt-BR"), Map.entry("pt", "pt-PT"), Map.entry("pt-pt", "pt-PT"),
            Map.entry("ru", "ru"), Map.entry("pl", "pl"), Map.entry("tr", "tr"), Map.entry("uk", "uk"),
            Map.entry("th", "th"), Map.entry("vi", "vi"), Map.entry("id", "id"), Map.entry("ar", "ar"),
            Map.entry("cs", "cs"), Map.entry("da", "da"), Map.entry("nl", "nl"), Map.entry("fi", "fi"),
            Map.entry("el", "el"), Map.entry("hu", "hu"), Map.entry("no", "no"), Map.entry("nb", "no"),
            Map.entry("ro", "ro"), Map.entry("sv", "sv"), Map.entry("bg", "bg"));

    /**
     * 목록 JSON의 content(게임 칸 HTML). 칸마다 게임 id, 주소, 이름, 평가 수(평점순 목록에만 있다).
     */
    public List<ItchListing> parseListing(String contentHtml) {
        List<ItchListing> listings = new ArrayList<>();
        if (contentHtml == null || contentHtml.isBlank()) {
            return listings;
        }
        Matcher start = CELL_START.matcher(contentHtml);
        List<int[]> cells = new ArrayList<>();
        while (start.find()) {
            cells.add(new int[] {start.start(), 0});
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (int i = 0; i < cells.size(); i++) {
            int from = cells.get(i)[0];
            int to = i + 1 < cells.size() ? cells.get(i + 1)[0] : contentHtml.length();
            String cell = contentHtml.substring(from, to);
            Matcher id = CELL_START.matcher(cell);
            Matcher title = CELL_TITLE.matcher(cell);
            // 이름 링크가 없는 칸(광고 등)은 건너뛴다
            if (!id.find() || !title.find()) {
                continue;
            }
            long itchId = Long.parseLong(id.group(1));
            String url = HtmlUtils.htmlUnescape(title.group(1));
            if (!seen.add(itchId) || !ItchUrls.isGameUrl(url)) {
                continue;
            }
            Matcher rating = CELL_RATING_COUNT.matcher(cell);
            Integer ratingCount = rating.find() ? Integer.valueOf(rating.group(1).replace(",", "")) : null;
            listings.add(new ItchListing(itchId, url, HtmlUtils.htmlUnescape(title.group(2)).strip(), ratingCount));
        }
        return listings;
    }

    /**
     * @throws ItchPageParseException 게임 id(itch:path)나 이름이 없으면
     */
    public ParsedItchGame parseGame(String html) {
        if (html == null) {
            throw new ItchPageParseException("Empty page");
        }
        Map<String, String> meta = metaTags(html);
        String path = meta.getOrDefault("itch:path", "");
        if (!path.matches("games/\\d+")) {
            throw new ItchPageParseException("Not a game page (itch:path=" + path + ")");
        }
        long itchId = Long.parseLong(path.substring("games/".length()));

        JsonNode product = productJsonLd(html);
        String title = firstText(text(product, "name"), meta.get("og:title"));
        if (title == null) {
            throw new ItchPageParseException("Game title not found: itchId=" + itchId);
        }
        String description = firstText(text(product, "description"), meta.get("og:description"));
        Integer ratingCount = product != null && product.path("aggregateRating").path("ratingCount").isNumber()
                ? product.path("aggregateRating").path("ratingCount").asInt() : null;

        Map<String, String> info = infoTable(html);
        List<String> tags = new ArrayList<>(new LinkedHashSet<>(concat(
                linkTexts(info.get("genre")), linkTexts(info.get("tags")))));
        List<String> authors = linkTexts(info.getOrDefault("author", info.get("authors")));

        return new ParsedItchGame(itchId, title, description, blankToNull(meta.get("og:image")), ratingCount,
                releaseDate(info), tags, languages(info.get("languages")), authors, screenshots(html));
    }

    /** itch.io에는 한국어 표기가 없어 "2026년 9월 16일" 모양으로 만든다 */
    public static String koreanDateText(LocalDate date) {
        return date == null ? null
                : date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    public static String englishDateText(LocalDate date) {
        return date == null ? null : EN_DATE.format(date);
    }

    private static Map<String, String> metaTags(String html) {
        Map<String, String> meta = new HashMap<>();
        Matcher tag = META.matcher(html);
        while (tag.find()) {
            Map<String, String> attributes = attributes(tag.group());
            String key = attributes.getOrDefault("property", attributes.get("name"));
            if (key != null && attributes.containsKey("content")) {
                meta.putIfAbsent(key, attributes.get("content"));
            }
        }
        return meta;
    }

    /** 태그 안의 속성들 (이름은 소문자, 값은 HTML 엔티티를 푼 값) */
    private static Map<String, String> attributes(String tag) {
        Map<String, String> attributes = new HashMap<>();
        Matcher attribute = ATTRIBUTE.matcher(tag);
        while (attribute.find()) {
            attributes.putIfAbsent(attribute.group(1).toLowerCase(Locale.ROOT), HtmlUtils.htmlUnescape(attribute.group(2)));
        }
        return attributes;
    }

    private static JsonNode productJsonLd(String html) {
        Matcher script = LD_JSON.matcher(html);
        while (script.find()) {
            try {
                JsonNode node = JSON.readTree(script.group(1));
                if ("Product".equals(node.path("@type").asString(""))) {
                    return node;
                }
            } catch (JacksonException ignored) {
                // 다른 구조화 데이터(빵부스러기 등)나 깨진 JSON은 건너뛴다
            }
        }
        return null;
    }

    /** "More information" 표: 소문자 항목 이름 → 칸 HTML */
    private static Map<String, String> infoTable(String html) {
        Map<String, String> rows = new LinkedHashMap<>();
        Matcher row = INFO_ROW.matcher(html);
        while (row.find()) {
            rows.putIfAbsent(row.group(1).strip().toLowerCase(Locale.ROOT), row.group(2));
        }
        return rows;
    }

    /** 출시일 항목이 있으면 그것, 없으면 처음 공개한 날. 마지막 갱신일(Updated)은 쓰지 않는다 */
    private static LocalDate releaseDate(Map<String, String> info) {
        for (String key : List.of("release date", "published")) {
            String cell = info.get(key);
            if (cell == null) {
                continue;
            }
            Matcher abbr = ABBR_TITLE.matcher(cell);
            if (abbr.find()) {
                // "16 September 2026 @ 03:22 UTC"
                String date = abbr.group(1).split("@", 2)[0].strip();
                try {
                    return LocalDate.parse(date, ITCH_DATE);
                } catch (DateTimeParseException ignored) {
                    // 모양이 다르면 출시일 없이 둔다
                }
            }
        }
        return null;
    }

    private static List<String> languages(String cell) {
        if (cell == null) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        Matcher link = LANGUAGE_LINK.matcher(cell);
        while (link.find()) {
            String code = LANGUAGES.get(link.group(1).toLowerCase(Locale.ROOT));
            if (code != null) {
                codes.add(code);
            }
        }
        return List.copyOf(codes);
    }

    private static List<SteamGameData.Media> screenshots(String html) {
        int start = html.indexOf("class=\"screenshot_list\"");
        if (start < 0) {
            return List.of();
        }
        int end = html.indexOf("</div>", start);
        String section = html.substring(start, end < 0 ? html.length() : end);
        List<SteamGameData.Media> media = new ArrayList<>();
        Matcher shot = SCREENSHOT.matcher(section);
        while (shot.find() && media.size() < MAX_SCREENSHOTS) {
            String original = attributes(shot.group(1)).get("href");
            Map<String, String> img = attributes(shot.group(2));
            String thumbnail = img.containsKey("src") ? img.get("src")
                    : img.getOrDefault("srcset", "").split("\\s+", 2)[0];
            if (original != null && !original.isBlank()) {
                media.add(new SteamGameData.Media(MediaType.SCREENSHOT, original, blankToNull(thumbnail)));
            }
        }
        return media;
    }

    private static List<String> linkTexts(String cell) {
        if (cell == null) {
            return List.of();
        }
        List<String> texts = new ArrayList<>();
        Matcher link = LINK_TEXT.matcher(cell);
        while (link.find()) {
            String text = HtmlUtils.htmlUnescape(link.group(1)).strip();
            if (!text.isEmpty()) {
                texts.add(text);
            }
        }
        return texts;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private static String text(JsonNode node, String field) {
        return node == null ? null : blankToNull(node.path(field).asString(null));
    }

    private static String firstText(String first, String second) {
        return first != null ? first : blankToNull(second);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
