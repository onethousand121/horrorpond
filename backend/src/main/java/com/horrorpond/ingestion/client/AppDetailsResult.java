package com.horrorpond.ingestion.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public sealed interface AppDetailsResult {

    /**
     * @param dataJson 응답의 {appid}.data 노드만 직렬화한 JSON
     */
    record Found(String dataJson) implements AppDetailsResult {

        private static final JsonMapper JSON = JsonMapper.builder().build();
        private static final String RECOMMENDATIONS = "recommendations";
        /** 영어 텍스트를 담는 키. Steam 응답에는 없는 이름이라 겹치지 않는다 */
        public static final String ENGLISH = "english";

        /**
         * appdetails는 리뷰가 꽤 쌓인 게임도 recommendations를 빼고 줄 때가 있다(주로 신작).
         */
        public boolean hasReviewCount() {
            return JSON.readTree(dataJson).has(RECOMMENDATIONS);
        }

        /**
         * appreviews에서 받은 리뷰 수를 appdetails와 같은 모양({"recommendations":{"total":N}})으로 채운다.
         * 파서는 출처를 몰라도 되고, 리뷰 수가 바뀌면 payload 해시도 바뀌어 다시 정규화된다.
         */
        public Found withReviewCount(int total) {
            ObjectNode data = (ObjectNode) JSON.readTree(dataJson);
            data.putObject(RECOMMENDATIONS).put("total", total);
            return new Found(data.toString());
        }

        /**
         * 영어 appdetails(l=english)에서 이름·짧은 소개·출시일 텍스트만 골라 {"english":{...}}로 붙인다.
         * 원본 스냅샷 하나로 정규화를 재실행할 수 있게 같은 payload에 둔다.
         */
        public Found withEnglish(String englishDataJson) {
            ObjectNode data = (ObjectNode) JSON.readTree(dataJson);
            JsonNode english = JSON.readTree(englishDataJson);
            ObjectNode target = data.putObject(ENGLISH);
            copyText(english.get("name"), target, "name");
            copyText(english.get("short_description"), target, "short_description");
            copyText(english.path("release_date").get("date"), target, "release_date");
            return new Found(data.toString());
        }

        private static void copyText(JsonNode value, ObjectNode target, String field) {
            if (value != null && value.isString() && !value.asString().isBlank()) {
                target.put(field, value.asString());
            }
        }
    }

    /**
     * Steam이 success=false로 응답했다 (삭제/지역 제한/존재하지 않는 appid).
     */
    record NotFound() implements AppDetailsResult {
    }
}
