-- "지금 뜨는" 큐레이터 조정: PINNED(맨 앞에 고정) / EXCLUDED(빼기) / NULL(자동). 직전 버전 앱은 이 컬럼을 쓰지 않는다.
ALTER TABLE game
    ADD COLUMN trending_override VARCHAR(10) CHECK (trending_override IN ('PINNED', 'EXCLUDED'));

-- 표가 많은 순 상위 n개 태그 안에 Horror 계열 태그가 있는지. 태그가 아직 없으면(신작) 공포로 본다
CREATE OR REPLACE FUNCTION horror_in_top_tags(tags VARCHAR[], n INT) RETURNS BOOLEAN
    LANGUAGE sql IMMUTABLE AS
$$
SELECT coalesce(cardinality(tags), 0) = 0
    OR EXISTS (SELECT 1 FROM unnest(tags[1:n]) AS t WHERE t LIKE '%Horror%')
$$;
