-- 게임별 하루 1건 리뷰 수 기록 (트렌드·급상승 계산용). store로 Steam/itch 값을 따로 쌓는다.
-- 추가만 하므로 직전 버전 앱과 호환된다.
CREATE TABLE game_metric_daily (
    game_id      BIGINT      NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    store        VARCHAR(20) NOT NULL CHECK (store IN ('STEAM','ITCH')),
    captured_on  DATE        NOT NULL,
    review_count INT         NOT NULL CHECK (review_count >= 0),
    captured_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (game_id, store, captured_on)
);
CREATE INDEX idx_game_metric_daily_captured_on ON game_metric_daily (captured_on);

-- 리뷰 수 기록 단계(METRICS). 허용 값만 넓힌다.
-- 직전 버전 앱은 이 값을 쓰지 않는다. 단, METRICS job이 생긴 뒤 직전 버전으로 되돌리면
-- 그 앱의 job 목록 조회(GET /api/admin/ingestion/jobs)가 모르는 enum 값을 읽게 된다.
ALTER TABLE ingestion_job DROP CONSTRAINT ingestion_job_type_check;
ALTER TABLE ingestion_job
    ADD CONSTRAINT ingestion_job_type_check
        CHECK (type IN ('DISCOVERY', 'ENRICHMENT', 'NORMALIZE', 'METRICS'));
