-- ===== 마스터 =====
CREATE TABLE genre (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    slug          VARCHAR(60)  NOT NULL UNIQUE,
    description   TEXT,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE developer (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    slug        VARCHAR(220) NOT NULL UNIQUE,
    website_url VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ===== Game 애그리거트 =====
CREATE TABLE game (
    id                BIGSERIAL PRIMARY KEY,
    source            VARCHAR(20)  NOT NULL CHECK (source IN ('STEAM','ITCH','MANUAL')),
    external_id       VARCHAR(50),
    slug              VARCHAR(220) NOT NULL UNIQUE,
    title             VARCHAR(300) NOT NULL,
    short_description TEXT,
    header_image_url  VARCHAR(500),
    release_date      DATE,
    release_date_text VARCHAR(100),
    coming_soon       BOOLEAN      NOT NULL DEFAULT false,
    status            VARCHAR(20)  NOT NULL DEFAULT 'CANDIDATE'
                      CHECK (status IN ('CANDIDATE','PUBLISHED','HIDDEN')),
    published_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_game_source_external UNIQUE (source, external_id),
    CONSTRAINT ck_game_external_required CHECK (source = 'MANUAL' OR external_id IS NOT NULL)
);
CREATE INDEX idx_game_published ON game (published_at DESC) WHERE status = 'PUBLISHED';

CREATE TABLE game_genre (
    game_id  BIGINT NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre(id),
    PRIMARY KEY (game_id, genre_id)
);
CREATE INDEX idx_game_genre_genre ON game_genre (genre_id);

CREATE TABLE game_developer (
    game_id      BIGINT      NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    developer_id BIGINT      NOT NULL REFERENCES developer(id),
    role         VARCHAR(20) NOT NULL CHECK (role IN ('DEVELOPER','PUBLISHER')),
    PRIMARY KEY (game_id, developer_id, role)
);
CREATE INDEX idx_game_developer_developer ON game_developer (developer_id);

CREATE TABLE game_media (
    id            BIGSERIAL PRIMARY KEY,
    game_id       BIGINT       NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    type          VARCHAR(20)  NOT NULL CHECK (type IN ('SCREENSHOT','TRAILER')),
    url           VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    sort_order    INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_game_media_game ON game_media (game_id, sort_order);

CREATE TABLE store_link (
    id      BIGSERIAL PRIMARY KEY,
    game_id BIGINT       NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    store   VARCHAR(20)  NOT NULL,
    url     VARCHAR(500) NOT NULL,
    CONSTRAINT uq_store_link UNIQUE (game_id, store)
);

-- ===== CurationArticle 애그리거트 =====
CREATE TABLE curation_article (
    id                 BIGSERIAL PRIMARY KEY,
    game_id            BIGINT       NOT NULL UNIQUE REFERENCES game(id),
    title              VARCHAR(200) NOT NULL,
    one_liner          VARCHAR(120) NOT NULL,
    body               TEXT         NOT NULL,
    highlights         TEXT[]       NOT NULL DEFAULT '{}',
    sponsored          BOOLEAN      NOT NULL DEFAULT false,
    sponsor_disclosure VARCHAR(300),
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                       CHECK (status IN ('DRAFT','PUBLISHED')),
    published_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_highlights_max CHECK (cardinality(highlights) <= 5),
    CONSTRAINT ck_highlights_on_publish
        CHECK (status = 'DRAFT' OR cardinality(highlights) >= 1),
    CONSTRAINT ck_sponsor_disclosure
        CHECK (sponsored = false OR length(trim(coalesce(sponsor_disclosure,''))) > 0)
);

-- ===== Ingestion (Game과 FK 없음) =====
CREATE TABLE steam_app_seed (
    appid           INT          PRIMARY KEY,
    discovered_by   VARCHAR(20)  NOT NULL CHECK (discovered_by IN ('STEAMSPY_TAG','MANUAL')),
    discovered_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_fetched_at TIMESTAMPTZ,
    fetch_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (fetch_status IN ('PENDING','OK','NOT_FOUND','FAILED')),
    fail_count      INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_seed_fetch ON steam_app_seed (fetch_status, last_fetched_at);

CREATE TABLE steam_raw_snapshot (
    appid           INT         PRIMARY KEY,
    payload         JSONB       NOT NULL,
    payload_hash    CHAR(64)    NOT NULL,
    normalized_hash CHAR(64),
    fetched_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE ingestion_job (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('DISCOVERY','ENRICHMENT','NORMALIZE')),
    trigger_type    VARCHAR(20) NOT NULL CHECK (trigger_type IN ('SCHEDULED','MANUAL')),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING','SUCCEEDED','FAILED')),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    processed_count INT         NOT NULL DEFAULT 0,
    failed_count    INT         NOT NULL DEFAULT 0,
    error_message   TEXT
);

-- ===== ShedLock =====
CREATE TABLE shedlock (
    name       VARCHAR(64)  PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
