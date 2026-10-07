package com.horrorpond.common.domain;

import java.util.Locale;

/**
 * 공개 사이트 언어. 영어 값이 없으면 한국어(기본) 값을 쓴다.
 */
public enum Language {
    KO, EN;

    /** 요청 파라미터 검증용 */
    public static final String PARAM_PATTERN = "(?i)ko|en";

    /** "ko" / "en" (대소문자 무시). 모르는 값이면 IllegalArgumentException */
    public static Language from(String code) {
        return valueOf(code.strip().toUpperCase(Locale.ROOT));
    }

    public String pick(String korean, String english) {
        return this == EN && english != null && !english.isBlank() ? english : korean;
    }
}
