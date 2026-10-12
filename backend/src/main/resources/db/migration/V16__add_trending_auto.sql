-- "지금 뜨는" 고정을 매주 조사 반영(자동)이 했는지. 자동 고정만 다음 반영 때 풀고, 관리자가 직접 고정한 건 건드리지 않는다.
-- 직전 버전 앱은 이 컬럼을 쓰지 않는다.
ALTER TABLE game ADD COLUMN trending_auto BOOLEAN NOT NULL DEFAULT false;
