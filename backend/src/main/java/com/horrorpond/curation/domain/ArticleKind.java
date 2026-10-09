package com.horrorpond.curation.domain;

/**
 * PICK: 주인장 추천 (추천 뱃지, 홈 "주인장 추천" 줄). 장점 포인트가 1개 이상 있어야 공개한다.
 * REVIEW: 플레이 후기 (후기 뱃지). 추천은 아니므로 추천 목록에 나오지 않고, 장점 포인트 없이도 공개한다.
 */
public enum ArticleKind {
    PICK, REVIEW
}
