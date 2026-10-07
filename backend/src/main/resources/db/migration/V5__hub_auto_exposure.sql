-- 태그 배열은 VARCHAR[]: 검색 파라미터(String[])가 varchar[]로 바인딩되므로 && 연산자 타입을 맞춘다
-- 공포게임 허브: 수집된 공포게임을 자동 노출한다 (자동 노출 규칙은 Game.isPubliclyVisible).
-- 추가만 하므로 직전 버전 앱과 호환된다.

-- Steam 소유 필드: 리뷰 수(appdetails recommendations.total), 성인 콘텐츠 여부(content_descriptors 3/4),
-- SteamSpy 상위 태그(서브장르 자동 분류용)
ALTER TABLE game
    ADD COLUMN review_count INT,
    ADD COLUMN adult        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN tags         VARCHAR(100)[] NOT NULL DEFAULT '{}';
CREATE INDEX idx_game_tags ON game USING GIN (tags);

-- enrichment가 SteamSpy에서 받은 상위 태그. normalize가 game.tags로 옮긴다
ALTER TABLE steam_app_seed ADD COLUMN spy_tags VARCHAR(100)[];

-- 장르와 SteamSpy 태그 매핑: 큐레이터가 붙인 장르가 없어도 태그로 장르 목록에 나온다
ALTER TABLE genre ADD COLUMN steam_tags VARCHAR(100)[] NOT NULL DEFAULT '{}';
UPDATE genre SET steam_tags = '{"Psychological Horror"}' WHERE slug = 'psychological';
UPDATE genre SET steam_tags = '{"Survival Horror"}' WHERE slug = 'survival';
UPDATE genre SET steam_tags = '{"Supernatural","Demons","Occult","Ghosts"}' WHERE slug = 'occult';
UPDATE genre SET steam_tags = '{"Analog Horror","Found Footage","PSX"}' WHERE slug = 'analog';
UPDATE genre SET steam_tags = '{"Lovecraftian","Cosmic Horror"}' WHERE slug = 'cosmic';

-- 기존 게임의 리뷰 수/성인 여부를 원본 스냅샷에서 다시 뽑도록 재정규화를 유도
UPDATE steam_raw_snapshot SET normalized_hash = NULL;
