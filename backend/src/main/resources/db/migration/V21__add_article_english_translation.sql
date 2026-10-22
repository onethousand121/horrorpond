-- 큐레이터 글 영어 번역 (선택). 모두 nullable이라 직전 버전 애플리케이션과 호환된다.
-- en_source_digest: 번역할 때의 한국어 글 요약값. 한국어 글이 바뀌면 달라져 "번역 갱신 필요"로 보인다.
ALTER TABLE curation_article ADD COLUMN title_en VARCHAR(200);
ALTER TABLE curation_article ADD COLUMN one_liner_en VARCHAR(120);
ALTER TABLE curation_article ADD COLUMN body_en TEXT;
ALTER TABLE curation_article ADD COLUMN highlights_en TEXT[];
ALTER TABLE curation_article ADD COLUMN en_source_digest VARCHAR(64);
