package com.horrorpond.ingestion.application;

/**
 * itch.io 페이지 모양을 줄인 테스트용 HTML (2026-10 기준 실제 페이지 구조).
 */
public final class ItchFixtures {

    private ItchFixtures() {
    }

    /** 평점순 목록의 게임 칸 하나. ratingCount가 null이면 평가 칸 없음 */
    public static String listingCell(long itchId, String url, String title, Integer ratingCount) {
        String rating = ratingCount == null ? "" : """
                <div><div data-tooltip="4.8 average rating" class="game_rating" tabindex="0">\
                <span class="rating_count">(%,d<span class="screenreader_only"> total ratings</span>)</span></div></div>"""
                .formatted(ratingCount);
        return """
                <div data-game_id="%d" dir="auto" class="game_cell has_cover lazy_images"><div class="game_thumb">\
                <a data-label="game:%d:thumb" href="%s" class="thumb_link game_link"><img data-lazy_src="x.png"/></a>\
                <div class="game_cell_tools"><a data-game_id="%d" data-register_action="add_to_collection" href="/g/x">Add</a></div></div>\
                <div class="game_cell_data"><div class="game_title"><a data-action="game_grid" data-label="game:%d:title" \
                class="title game_link" href="%s">%s</a></div>%s</div></div>"""
                .formatted(itchId, itchId, url, itchId, itchId, url, title, rating);
    }

    /** 목록 JSON (format=json). content는 게임 칸 HTML */
    public static String listingJson(String... cells) {
        String content = String.join("", cells).replace("\\", "\\\\").replace("\"", "\\\"").replace("/", "\\/");
        return "{\"page\":1,\"content\":\"" + content + "\",\"num_items\":" + cells.length + "}";
    }

    public static String gamePage(long itchId, String title, String description, Integer ratingCount) {
        String rating = ratingCount == null ? ""
                : ",\"aggregateRating\":{\"ratingValue\":\"4.9\",\"ratingCount\":" + ratingCount
                        + ",\"@type\":\"AggregateRating\"}";
        return """
                <!DOCTYPE html><html><head>\
                <meta content="games/%d" name="itch:path"/>\
                <meta property="og:image" content="https://img.itch.zone/cover/original/c.png"/>\
                <meta content="itch.io" property="og:site_name"/>\
                <script type="application/ld+json">{"@type":"BreadcrumbList","itemListElement":[]}</script>\
                <script type="application/ld+json">{"name":"%s","@type":"Product"%s,"description":"%s","@context":"http:\\/\\/schema.org\\/"}</script>\
                </head><body>\
                <div class="screenshot_list"><a data-image_lightbox="true" target="_blank" href="https://img.itch.zone/s1/original/a.png">\
                <img data-screenshot_id="1" srcset="https://img.itch.zone/s1/347x500/a.png 1x, https://img.itch.zone/s1/794x1000/a.png 2x" class="screenshot"/></a>\
                <a target="_blank" href="https://img.itch.zone/s2/original/b.png" data-image_lightbox="true">\
                <img src="https://img.itch.zone/s2/347x500/b.png" class="screenshot"/></a></div>\
                <table><tbody>\
                <tr><td>Updated</td><td><abbr title="16 September 2026 @ 06:41 UTC">23 days ago</abbr></td></tr>\
                <tr><td>Published</td><td><abbr title="03 March 2024 @ 11:00 UTC">Mar 03, 2024</abbr></td></tr>\
                <tr><td>Author</td><td><a href="https://dev.itch.io">Dev Studio</a></td></tr>\
                <tr><td>Genre</td><td><a href="https://itch.io/games/genre-adventure">Adventure</a></td></tr>\
                <tr><td>Tags</td><td><a href="https://itch.io/games/tag-horror">Horror</a>, \
                <a href="https://itch.io/games/tag-psychological-horror">Psychological Horror</a></td></tr>\
                <tr><td>Languages</td><td><a href="https://itch.io/games/lang-en">English</a>, \
                <a href="https://itch.io/games/lang-ko">Korean</a>, <a href="https://itch.io/games/lang-pt-br">Portuguese (Brazil)</a>, \
                <a href="https://itch.io/games/lang-xx">Unknown</a></td></tr>\
                </tbody></table></body></html>"""
                .formatted(itchId, title, rating, description);
    }
}
