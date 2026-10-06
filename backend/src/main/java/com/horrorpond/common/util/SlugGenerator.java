package com.horrorpond.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 제목을 URL slug(소문자 영숫자 + 하이픈)로 변환한다.
 * 영문/숫자 외 문자는 제거하고, 결과가 비면 "game"을 쓴다.
 */
public final class SlugGenerator {

    static final String FALLBACK = "game";
    static final int MAX_LENGTH = 200;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern APOSTROPHES = Pattern.compile("['’]");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

    private SlugGenerator() {
    }

    public static String slugify(String title) {
        if (title == null) {
            return FALLBACK;
        }
        String s = Normalizer.normalize(title, Normalizer.Form.NFKD);
        s = DIACRITICS.matcher(s).replaceAll("");
        s = s.toLowerCase(Locale.ROOT);
        s = APOSTROPHES.matcher(s).replaceAll("");
        s = NON_ALNUM.matcher(s).replaceAll("-");
        s = EDGE_HYPHENS.matcher(s).replaceAll("");
        if (s.length() > MAX_LENGTH) {
            s = EDGE_HYPHENS.matcher(s.substring(0, MAX_LENGTH)).replaceAll("");
        }
        return s.isEmpty() ? FALLBACK : s;
    }

    /**
     * Steam 후보 게임용 slug: "{slug}-{appid}".
     */
    public static String forSteamCandidate(String title, int appid) {
        return slugify(title) + "-" + appid;
    }
}
