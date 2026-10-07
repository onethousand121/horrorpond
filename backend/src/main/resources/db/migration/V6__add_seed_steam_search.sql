-- Steam 스토어 검색으로 발견한 seed (신작·출시 예정작). 허용 값만 넓힌다.
-- 직전 버전 앱은 이 값을 쓰지 않는다. 단, STEAM_SEARCH seed가 생긴 뒤 직전 버전으로 되돌리면
-- 그 앱의 enrichment 갱신 조회가 모르는 enum 값을 읽게 되므로, 롤백은 다음 정기 수집(04:00) 전까지만 안전하다.
ALTER TABLE steam_app_seed DROP CONSTRAINT steam_app_seed_discovered_by_check;
ALTER TABLE steam_app_seed
    ADD CONSTRAINT steam_app_seed_discovered_by_check
        CHECK (discovered_by IN ('STEAMSPY_TAG', 'MANUAL', 'STEAM_SEARCH'));
