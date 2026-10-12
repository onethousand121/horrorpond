-- 참고 영상: 주인장 영상이 없을 때 보여주는 다른 채널의 플레이 영상. 기존 영상은 모두 주인장 영상(KEEPER)이다.
-- 직전 버전 앱은 이 컬럼을 모르고, 기본값 덕분에 그대로 주인장 영상을 저장할 수 있다.
ALTER TABLE play_video
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'KEEPER' CHECK (kind IN ('KEEPER', 'REFERENCE')),
    ADD COLUMN channel_name VARCHAR(100);
