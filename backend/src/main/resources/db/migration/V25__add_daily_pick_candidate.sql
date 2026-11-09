-- "오늘의 게임" 후보. 메인에 하루에 한 게임씩 돌아가며 크게 소개한다 (후보가 없으면 주인장 추천 게임으로 대신).
-- featured_on: 그 날짜에 이 게임을 고정 (출시일 맞춤 등). 새 테이블만 추가하므로 직전 버전과 호환된다
CREATE TABLE daily_pick_candidate (
    id          BIGSERIAL    PRIMARY KEY,
    game_id     BIGINT       NOT NULL UNIQUE REFERENCES game(id),
    intro       VARCHAR(120) NOT NULL,
    intro_en    VARCHAR(200),
    sponsored   BOOLEAN      NOT NULL DEFAULT FALSE,
    featured_on DATE         UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
