package com.horrorpond.curation.application;

/**
 * 홈 상단의 숫자 한 줄. 모두 공개 사이트에 보이는 게임 기준, 날짜는 한국 시간.
 *
 * @param releasedToday     오늘 출시
 * @param releasingTomorrow 내일 출시 예정
 */
public record HubStatsResponse(long releasedToday, long releasingTomorrow) {
}
