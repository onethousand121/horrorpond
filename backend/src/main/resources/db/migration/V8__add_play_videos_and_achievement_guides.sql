-- 게임별 플레이 영상과 업적 공략(업적 이름·설명·공략 영상). 큐레이터가 관리하고, 없으면 사이트에 보이지 않는다.
-- 새 테이블만 추가하므로 직전 버전 앱과 호환된다.
CREATE TABLE play_video (
    id         BIGSERIAL    PRIMARY KEY,
    game_id    BIGINT       NOT NULL REFERENCES game(id),
    youtube_id VARCHAR(11)  NOT NULL,
    title      VARCHAR(200),
    sort_order INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_play_video_game ON play_video (game_id, sort_order);

CREATE TABLE achievement_guide (
    id          BIGSERIAL    PRIMARY KEY,
    game_id     BIGINT       NOT NULL REFERENCES game(id),
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    youtube_id  VARCHAR(11),
    sort_order  INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_achievement_guide_game ON achievement_guide (game_id, sort_order);
