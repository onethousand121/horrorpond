-- itch.io 수집. 인기(평가 수 기준) 공포게임과 관리자가 고른 게임만 들어온다.
-- 직전 버전 앱은 이 테이블과 ITCH job을 쓰지 않는다.
CREATE TABLE itch_game_seed (
    itch_id         BIGINT       PRIMARY KEY,
    url             VARCHAR(500) NOT NULL,
    discovered_by   VARCHAR(20)  NOT NULL CHECK (discovered_by IN ('TOP_RATED', 'MANUAL')),
    rating_count    INT CHECK (rating_count >= 0),
    discovered_at   TIMESTAMPTZ  NOT NULL,
    last_fetched_at TIMESTAMPTZ,
    fetch_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (fetch_status IN ('PENDING', 'OK', 'NOT_FOUND', 'FAILED')),
    fail_count      INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_itch_seed_fetch ON itch_game_seed (fetch_status, last_fetched_at);

ALTER TABLE ingestion_job DROP CONSTRAINT ingestion_job_type_check;
ALTER TABLE ingestion_job
    ADD CONSTRAINT ingestion_job_type_check
        CHECK (type IN ('DISCOVERY', 'ENRICHMENT', 'NORMALIZE', 'METRICS', 'TRANSLATE', 'ITCH'));
