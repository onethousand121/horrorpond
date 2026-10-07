package com.horrorpond.curation.repository;

/**
 * 출시 시점 필터. 허브 홈의 "출시 예정" / "최근 출시" 줄에 쓴다.
 */
public enum ReleaseWindow {
    /** 출시 예정 (Steam coming_soon). 출시일이 가까운 순 */
    UPCOMING,
    /** 최근 출시 (오늘 기준 RECENT_DAYS일 이내) */
    RECENT,
    /** 이번 주 출시 (오늘 포함 최근 WEEK_DAYS일) */
    THIS_WEEK;

    public static final int RECENT_DAYS = 90;
    public static final int WEEK_DAYS = 7;
}
