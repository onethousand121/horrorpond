package com.horrorpond.catalog.domain;

/**
 * "지금 뜨는" 목록의 큐레이터 조정. 없으면(null) 자동 기준(최근 7일 리뷰 증가량)을 따른다.
 * PINNED: 자동 기준과 상관없이 맨 앞에 둔다. EXCLUDED: 자동 기준을 넘어도 빼 둔다.
 */
public enum TrendingOverride {
    PINNED, EXCLUDED
}
