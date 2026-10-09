-- 한국어 소개가 없는(Steam 한국어 소개에 한글이 없는) 게임의 자동 번역(DeepL).
-- Steam 소개가 바뀌면 Game.applySteamData가 지우고 다음 번역 단계에서 다시 번역한다.
-- 추가만 하므로 직전 버전 앱과 호환된다.
ALTER TABLE game ADD COLUMN short_description_ko_auto TEXT;

-- 자동 번역 단계(TRANSLATE). 허용 값만 넓힌다 (V9와 같은 이유로 롤백 시 job 목록 조회만 영향).
ALTER TABLE ingestion_job DROP CONSTRAINT ingestion_job_type_check;
ALTER TABLE ingestion_job
    ADD CONSTRAINT ingestion_job_type_check
        CHECK (type IN ('DISCOVERY', 'ENRICHMENT', 'NORMALIZE', 'METRICS', 'TRANSLATE'));
