-- 매시간 출시 확인(RELEASE_CHECK) job. 허용 값만 넓힌다 (V9와 같은 이유로 롤백 시 job 목록 조회만 영향).
ALTER TABLE ingestion_job DROP CONSTRAINT ingestion_job_type_check;
ALTER TABLE ingestion_job
    ADD CONSTRAINT ingestion_job_type_check
        CHECK (type IN ('DISCOVERY', 'ENRICHMENT', 'NORMALIZE', 'METRICS', 'TRANSLATE', 'ITCH', 'RELEASE_CHECK'));
