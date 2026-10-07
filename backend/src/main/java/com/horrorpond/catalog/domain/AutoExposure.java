package com.horrorpond.catalog.domain;

import java.time.LocalDate;

/**
 * 수집된 공포게임(CANDIDATE)의 자동 노출 기준. 성인 콘텐츠가 아니면서 다음 중 하나면 노출한다.
 * <ul>
 *   <li>출시 예정</li>
 *   <li>출시한 지 newReleaseDays일 이내 (리뷰가 쌓이기 전 신작도 바로 보이게)</li>
 *   <li>Steam 리뷰가 minReviews개 이상</li>
 * </ul>
 */
public record AutoExposure(int minReviews, int newReleaseDays) {

    public boolean allows(boolean adult, boolean comingSoon, Integer reviewCount, LocalDate releaseDate,
                          LocalDate today) {
        if (adult) {
            return false;
        }
        return comingSoon
                || isNewRelease(releaseDate, today)
                || (reviewCount != null && reviewCount >= minReviews);
    }

    /** 출시일이 [today - newReleaseDays, today] 안에 있다 */
    public boolean isNewRelease(LocalDate releaseDate, LocalDate today) {
        return releaseDate != null && !releaseDate.isAfter(today) && !releaseDate.isBefore(newReleaseSince(today));
    }

    public LocalDate newReleaseSince(LocalDate today) {
        return today.minusDays(newReleaseDays);
    }
}
