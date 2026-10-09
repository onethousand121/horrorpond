package com.horrorpond.ingestion.client;

import com.horrorpond.common.domain.DomainValidationException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * itch.io 게임 주소. 서버가 대신 요청을 보내므로 {작성자}.itch.io/{게임} 모양만 받는다.
 */
public final class ItchUrls {

    private static final Pattern GAME_URL = Pattern.compile(
            "^(?:https?://)?([a-z0-9][a-z0-9_-]*)\\.itch\\.io/([a-z0-9][a-z0-9_-]*)/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private ItchUrls() {
    }

    /**
     * @return "https://{작성자}.itch.io/{게임}" (소문자)
     * @throws DomainValidationException itch.io 게임 주소가 아니면
     */
    public static String normalize(String url) {
        Matcher m = url == null ? null : GAME_URL.matcher(url.strip());
        if (m == null || !m.matches()) {
            throw new DomainValidationException("Not an itch.io game URL: " + url);
        }
        return "https://" + m.group(1).toLowerCase(Locale.ROOT) + ".itch.io/" + m.group(2).toLowerCase(Locale.ROOT);
    }

    public static boolean isGameUrl(String url) {
        return url != null && GAME_URL.matcher(url.strip()).matches();
    }
}
