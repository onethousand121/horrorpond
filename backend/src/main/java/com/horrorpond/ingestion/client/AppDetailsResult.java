package com.horrorpond.ingestion.client;

public sealed interface AppDetailsResult {

    /**
     * @param dataJson 응답의 {appid}.data 노드만 직렬화한 JSON
     */
    record Found(String dataJson) implements AppDetailsResult {
    }

    /**
     * Steam이 success=false로 응답했다 (삭제/지역 제한/존재하지 않는 appid).
     */
    record NotFound() implements AppDetailsResult {
    }
}
