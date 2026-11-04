package com.horrorpond.curation.domain;

/**
 * 업적 공략·엔딩의 선택적 영어 번역. 비어 있는 값은 번역 없음(영어 화면에 한국어를 보여 준다).
 */
public record EnglishText(String name, String description) {

    public static final EnglishText NONE = new EnglishText(null, null);

    public static EnglishText of(String name, String description) {
        return new EnglishText(name, description);
    }
}
