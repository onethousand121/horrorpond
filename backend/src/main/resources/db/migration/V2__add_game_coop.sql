ALTER TABLE game ADD COLUMN coop BOOLEAN NOT NULL DEFAULT false;
UPDATE steam_raw_snapshot SET normalized_hash = NULL;  -- 기존 게임 재정규화 유도
