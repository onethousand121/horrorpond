package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 유튜브 주소에서 영상 ID(11자)를 꺼낸다. 영상 ID만 넣어도 된다.
 * 지원: youtube.com/watch?v=, youtu.be/, /shorts/, /embed/, /live/ (m., www., music. 포함)
 */
public final class YoutubeVideoId {

    public static final int LENGTH = 11;

    private static final String ID = "([A-Za-z0-9_-]{11})";
    private static final Pattern BARE_ID = Pattern.compile("^" + ID + "$");
    private static final List<Pattern> URL_PATTERNS = List.of(
            Pattern.compile("^(?:https?://)?(?:www\\.|m\\.|music\\.)?youtube\\.com/watch\\?(?:.*&)?v=" + ID + "(?:[&#].*)?$"),
            Pattern.compile("^(?:https?://)?youtu\\.be/" + ID + "(?:[?&#].*)?$"),
            Pattern.compile("^(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/(?:shorts|embed|live)/" + ID + "(?:[/?&#].*)?$"));

    private YoutubeVideoId() {
    }

    public static String parse(String input) {
        if (input == null || input.isBlank()) {
            throw new DomainValidationException("YouTube URL must not be blank");
        }
        String value = input.strip();
        Matcher bare = BARE_ID.matcher(value);
        if (bare.matches()) {
            return bare.group(1);
        }
        for (Pattern pattern : URL_PATTERNS) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new DomainValidationException("Not a YouTube video URL: " + value);
    }
}
