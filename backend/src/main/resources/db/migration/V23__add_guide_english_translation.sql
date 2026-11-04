-- 업적 공략·엔딩의 선택적 영어 번역. 없으면 영어 화면에도 한국어를 보여 준다. 추가만 하므로 직전 버전과 호환된다
ALTER TABLE achievement_guide ADD COLUMN name_en VARCHAR(200);
ALTER TABLE achievement_guide ADD COLUMN description_en VARCHAR(500);
ALTER TABLE ending_guide ADD COLUMN name_en VARCHAR(100);
ALTER TABLE ending_guide ADD COLUMN description_en TEXT CHECK (char_length(description_en) <= 2000);
