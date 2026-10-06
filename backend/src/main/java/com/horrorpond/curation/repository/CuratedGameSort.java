package com.horrorpond.curation.repository;

public enum CuratedGameSort {
    /**
     * 최근 공개 순 (game.publishedAt desc)
     */
    LATEST,
    /**
     * 최근 출시 순 (releaseDate desc, 출시일 미상은 뒤로)
     */
    RELEASE
}
