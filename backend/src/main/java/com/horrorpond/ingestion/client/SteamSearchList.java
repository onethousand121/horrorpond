package com.horrorpond.ingestion.client;

/**
 * Steam 스토어 검색에서 가져오는 Horror 태그 목록.
 */
public enum SteamSearchList {
    /** 출시일 최신순 */
    NEW_RELEASES("sort_by", "Released_DESC"),
    /** 인기 출시 예정 (위시리스트 등 Steam 인기 기준) */
    POPULAR_UPCOMING("filter", "popularcomingsoon");

    final String paramName;
    final String paramValue;

    SteamSearchList(String paramName, String paramValue) {
        this.paramName = paramName;
        this.paramValue = paramValue;
    }
}
