package com.horrorpond.catalog.domain;

/**
 * 상점의 정가. 할인가는 자주 바뀌어 저장하지 않는다.
 *
 * @param free     무료 게임 (itch.io는 "원하는 만큼 내기"로 0원 다운로드가 되면 무료)
 * @param amount   정가의 최소 단위 금액 (Steam 원화는 100배, itch.io는 통화의 최소 단위·달러면 센트). 모르거나 출시 전이면 null
 * @param currency ISO 통화 코드 (KRW, USD, EUR …). amount가 없으면 null
 */
public record StorePrice(boolean free, Integer amount, String currency) {

    public static final StorePrice UNKNOWN = new StorePrice(false, null, null);

    public StorePrice {
        if (amount == null || amount < 0 || currency == null || currency.isBlank()) {
            amount = null;
            currency = null;
        }
    }
}
