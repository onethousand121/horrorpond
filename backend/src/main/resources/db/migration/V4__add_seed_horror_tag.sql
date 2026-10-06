-- SteamSpy Horror 태그 목록에는 Horror 표가 한 표라도 있는 게임이 모두 들어 있다(PUBG, Apex 등).
-- 게임별 상위 태그로 판정한 결과를 seed에 기록하고, NOT_HORROR는 enrichment에서 제외한다.
-- 기존 seed는 UNCHECKED로 시작해 다음 enrichment에서 판정된다. 직전 버전 앱은 이 컬럼을 쓰지 않는다.
ALTER TABLE steam_app_seed
    ADD COLUMN horror_tag            VARCHAR(20) NOT NULL DEFAULT 'UNCHECKED'
        CHECK (horror_tag IN ('UNCHECKED','HORROR','NOT_HORROR')),
    ADD COLUMN horror_tag_checked_at TIMESTAMPTZ;
