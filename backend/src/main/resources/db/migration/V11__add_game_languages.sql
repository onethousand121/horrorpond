-- Steam 지원 언어(코드, 예: ko, en, ja, zh-Hans). audio_languages는 음성까지 지원하는 언어.
-- 추가만 하므로 직전 버전 앱과 호환된다.
ALTER TABLE game
    ADD COLUMN languages       VARCHAR(20)[] NOT NULL DEFAULT '{}',
    ADD COLUMN audio_languages VARCHAR(20)[] NOT NULL DEFAULT '{}';

-- 원본 스냅샷에 supported_languages가 이미 있으므로, 다음 정규화 때 기존 게임 모두에 채우도록 재정규화를 유도
UPDATE steam_raw_snapshot SET normalized_hash = NULL;
