-- 큐레이션 글 종류: 추천(PICK) / 플레이 후기(REVIEW). 기존 글은 모두 추천이다.
-- 직전 버전 앱은 이 컬럼을 모르고, 기본값 덕분에 그대로 글을 저장할 수 있다.
ALTER TABLE curation_article
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'PICK' CHECK (kind IN ('PICK', 'REVIEW'));

-- 후기는 장점 포인트 없이도 공개할 수 있다
ALTER TABLE curation_article DROP CONSTRAINT ck_highlights_on_publish;
ALTER TABLE curation_article
    ADD CONSTRAINT ck_highlights_on_publish
        CHECK (status = 'DRAFT' OR kind = 'REVIEW' OR cardinality(highlights) >= 1);
