-- 영어 사이트용 텍스트. 모두 nullable 추가라 직전 버전 앱과 호환된다.
-- 게임은 다음 enrichment 갱신 때(l=english) 채워지고, 그전까지는 한국어 값을 쓴다.
ALTER TABLE game
    ADD COLUMN title_en             VARCHAR(300),
    ADD COLUMN short_description_en TEXT,
    ADD COLUMN release_date_text_en VARCHAR(100);

ALTER TABLE genre
    ADD COLUMN name_en        VARCHAR(50),
    ADD COLUMN description_en TEXT;

UPDATE genre SET name_en = 'Psychological', description_en = 'Dread, tension and pressure on the mind'
WHERE slug = 'psychological';
UPDATE genre SET name_en = 'Survival Horror', description_en = 'Scarce resources, survival, fight or flight'
WHERE slug = 'survival';
UPDATE genre SET name_en = 'Occult', description_en = 'Ghosts, folklore and religious horror'
WHERE slug = 'occult';
UPDATE genre SET name_en = 'Analog Horror', description_en = 'VHS, lo-fi visuals and found footage'
WHERE slug = 'analog';
UPDATE genre SET name_en = 'Cosmic Horror', description_en = 'Incomprehensible beings and vast dread'
WHERE slug = 'cosmic';
