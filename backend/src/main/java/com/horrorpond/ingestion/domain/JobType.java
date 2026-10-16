package com.horrorpond.ingestion.domain;

public enum JobType {
    DISCOVERY, ENRICHMENT, NORMALIZE, METRICS, TRANSLATE, ITCH,
    /** 매시간 출시 확인 (하루 1회 수집 단계가 아니다) */
    RELEASE_CHECK
}
