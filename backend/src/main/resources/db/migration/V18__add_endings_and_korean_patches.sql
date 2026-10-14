-- 엔딩 공략과 유저 한글패치. 큐레이터가 관리하고, 없으면 사이트에 보이지 않는다.
-- 컬럼·테이블 추가만 하므로 직전 버전 앱과 호환된다 (직전 앱은 이 컬럼·테이블을 쓰지 않는다).

-- 엔딩 구분: SINGLE(단일) / MULTIPLE(멀티) / NULL(표시 안 함)
ALTER TABLE game
    ADD COLUMN ending_type VARCHAR(10) CHECK (ending_type IN ('SINGLE', 'MULTIPLE'));

-- 멀티 엔딩 게임의 엔딩 목록 (이름·보는 방법·영상)
CREATE TABLE ending_guide (
    id          BIGSERIAL    PRIMARY KEY,
    game_id     BIGINT       NOT NULL REFERENCES game(id),
    name        VARCHAR(100) NOT NULL,
    description TEXT CHECK (char_length(description) <= 2000),
    youtube_id  VARCHAR(11),
    sort_order  INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ending_guide_game ON ending_guide (game_id, sort_order);

-- 유저가 만든 비공식 한국어 패치 (배포처 링크)
CREATE TABLE korean_patch (
    id         BIGSERIAL    PRIMARY KEY,
    game_id    BIGINT       NOT NULL REFERENCES game(id),
    url        VARCHAR(500) NOT NULL,
    author     VARCHAR(100),
    note       VARCHAR(200),
    sort_order INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_korean_patch_game ON korean_patch (game_id, sort_order);
