-- "지금 뜨는" 고정 게임의 순서 (관리자가 정함, 작을수록 앞). 고정이 아니거나 순서를 안 정했으면 NULL
-- 컬럼 추가만 하므로 직전 버전 앱과 호환된다.
ALTER TABLE game ADD COLUMN trending_pin_order INT;
