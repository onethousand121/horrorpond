package com.horrorpond.ingestion.application;

/**
 * itch.io 목록의 게임 칸 하나.
 *
 * @param ratingCount 평가 수. 평가가 없으면 null
 */
public record ItchListing(long itchId, String url, String title, Integer ratingCount) {
}
