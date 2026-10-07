package com.horrorpond.ingestion.domain;

/**
 * STEAMSPY_TAG: SteamSpy Horror 태그 목록 (전체, 갱신이 느려 신작이 늦게 들어온다)
 * STEAM_SEARCH: Steam 스토어 검색 (Horror 태그의 최신 출시작·인기 출시 예정작)
 * MANUAL: 관리자가 직접 추가
 */
public enum DiscoveredBy {
    STEAMSPY_TAG, MANUAL, STEAM_SEARCH
}
