package com.horrorpond.curation.application;

/**
 * 홈 상단의 숫자 한 줄. 모두 공개 사이트에 보이는 게임 기준.
 *
 * @param total            보관 중인(노출 중인) 공포게임 수
 * @param upcoming         출시 예정
 * @param releasedThisWeek 오늘 포함 최근 7일 출시
 */
public record HubStatsResponse(long total, long upcoming, long releasedThisWeek) {
}
