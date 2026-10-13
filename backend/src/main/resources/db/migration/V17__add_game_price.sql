-- 가격: 무료 여부와 정가(할인 전). 할인은 자주 바뀌어 7일 갱신으로는 틀리기 쉬워 저장하지 않는다.
-- price_amount는 최소 단위(Steam 원화는 100배, itch 달러는 센트). 직전 버전 앱은 이 컬럼을 쓰지 않는다.
ALTER TABLE game
    ADD COLUMN is_free        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN price_amount   INT CHECK (price_amount >= 0),
    ADD COLUMN price_currency VARCHAR(3);

-- 이미 받아 둔 Steam 응답에도 가격이 들어 있으므로, 다음 normalize가 모든 스냅샷을 한 번 다시 반영하게 한다
UPDATE steam_raw_snapshot SET normalized_hash = NULL;
