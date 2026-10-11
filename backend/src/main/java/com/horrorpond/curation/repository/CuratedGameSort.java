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
     * 지금 뜨는 순: 최근 {@link #TRENDING_WINDOW_DAYS}일 동안 늘어난 리뷰 수(itch.io는 평가 수) desc.
     * 기준점은 7~14일 전 기록 중 가장 큰 값이고, 출시한 지 7일이 안 된 게임은 0에서 센다. 기록이 없는 게임은 빠진다.
     * 표가 많은 순 상위 {@link #TRENDING_HORROR_TAG_RANK}개 태그에 Horror 계열이 있어야 한다 (itch.io는 공포 목록에서만 받으므로 예외).
     * 큐레이터가 고정한 게임(PINNED)은 맨 앞, 뺀 게임(EXCLUDED)은 나오지 않는다.
     */
    TRENDING;

    public static final int TRENDING_WINDOW_DAYS = 7;
    /** 기준점을 찾는 가장 오래된 기록 (리뷰 수 기록이 매일 모든 게임에 있지는 않다) */
    public static final int TRENDING_BASELINE_MAX_DAYS = 14;
    public static final int TRENDING_HORROR_TAG_RANK = 5;
}
