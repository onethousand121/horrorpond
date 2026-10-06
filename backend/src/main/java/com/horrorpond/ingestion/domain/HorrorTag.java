package com.horrorpond.ingestion.domain;

import java.util.List;

/**
 * SteamSpy로 발견한 seed가 실제 공포게임인지 판정한 결과.
 * SteamSpy Horror 태그 목록은 Horror 표가 한 표라도 있으면 포함하므로, 게임별 상위 태그로 다시 거른다.
 */
public enum HorrorTag {
    UNCHECKED, HORROR, NOT_HORROR;

    private static final String HORROR_KEYWORD = "Horror";

    /**
     * SteamSpy appdetails는 표가 많은 순으로 상위 태그(최대 20개)를 준다. 그 안에 Horror가 들어간 태그
     * (Horror, Psychological Horror, Survival Horror 등)가 있으면 공포게임이다.
     * 태그 정보가 없으면(신작 등) Horror 태그 목록에 있었다는 사실을 믿고 HORROR로 본다.
     */
    public static HorrorTag classify(List<String> topTags) {
        if (topTags.isEmpty() || topTags.stream().anyMatch(tag -> tag.contains(HORROR_KEYWORD))) {
            return HORROR;
        }
        return NOT_HORROR;
    }
}
