package com.horrorpond.curation.repository;

public enum CuratedGameSort {
    /**
     * 최근 사이트에 올라온 순 (재일 추천 목록이면 추천한 순)
     */
    LATEST,
    /**
     * 최근 출시 순 (releaseDate desc, 출시일 미상은 뒤로)
     */
    RELEASE,
    /**
     * 인기 순 (Steam 리뷰 수 desc). 누적이라 오래된 대작이 앞에 온다 (스테디셀러)
     */
    POPULAR,
    /**
     * 지금 뜨는 순: 최근 {@link #TRENDING_DAYS}일 안에 출시한 게임만, 출시 후 하루 평균 리뷰 수 desc.
     * 출시 직후 며칠은 분모를 {@link #TRENDING_MIN_DAYS}일로 잡아 리뷰 몇 개로 1위가 되지 않게 한다.
     * (리뷰 수 이력 game_metric_daily가 쌓이면 최근 7일 증가량 기준으로 바꾼다)
     */
    TRENDING;

    public static final int TRENDING_DAYS = 90;
    public static final int TRENDING_MIN_DAYS = 7;
}
