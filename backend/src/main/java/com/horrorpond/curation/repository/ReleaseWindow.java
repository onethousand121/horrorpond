package com.horrorpond.curation.repository;

/**
 * 출시 시점 필터. 허브 홈의 "출시 예정" / "최근 출시" 줄과 숫자 한 줄(오늘/내일 출시)에 쓴다.
 */
public enum ReleaseWindow {
    /** 출시 예정 (Steam coming_soon). 출시일이 가까운 순 */
    UPCOMING,
    /** 최근 출시 (오늘 기준 RECENT_DAYS일 이내) */
    RECENT,
    /** 오늘 출시 */
    TODAY,
    /** 내일 출시 예정 */
    TOMORROW;

    public static final int RECENT_DAYS = 90;
}
