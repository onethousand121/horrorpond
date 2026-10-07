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
     * 인기 순 (Steam 리뷰 수 desc)
     */
    POPULAR
}
