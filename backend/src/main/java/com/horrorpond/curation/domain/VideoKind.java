package com.horrorpond.curation.domain;

/**
 * KEEPER: 주인장(류재일) 플레이 영상. 영상 뱃지·주인장 추천 줄의 기준이다.
 * REFERENCE: 다른 채널의 참고 영상. 주인장 영상이 없을 때만 사이트에 보이고, 뱃지·추천 줄에는 쓰지 않는다.
 */
public enum VideoKind {
    KEEPER, REFERENCE
}
