package com.horrorpond.catalog.domain;

/**
 * 엔딩 구분. 없으면(null) 사이트에 엔딩 영역을 보여주지 않는다.
 * SINGLE: 엔딩이 하나뿐. MULTIPLE: 엔딩이 여러 개 (엔딩 공략 목록을 둘 수 있다).
 */
public enum EndingType {
    SINGLE, MULTIPLE
}
