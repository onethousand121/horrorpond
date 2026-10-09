package com.horrorpond.ingestion.domain;

/**
 * TOP_RATED: itch.io 공포 태그 평점순 목록에서 평가 수가 기준 이상인 게임
 * MANUAL: 관리자가 itch.io 주소로 직접 추가
 */
public enum ItchDiscoveredBy {
    TOP_RATED, MANUAL
}
