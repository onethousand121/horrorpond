-- 엔딩 설명(한국어·영어) 최대 길이를 2000자에서 5000자로 늘린다 (영어 번역은 같은 내용이어도 더 길다).
-- 제한을 넓히기만 하므로 2000자를 검사하는 직전 버전 애플리케이션과도 호환된다
ALTER TABLE ending_guide DROP CONSTRAINT ending_guide_description_check;
ALTER TABLE ending_guide ADD CONSTRAINT ending_guide_description_check CHECK (char_length(description) <= 5000);
ALTER TABLE ending_guide DROP CONSTRAINT ending_guide_description_en_check;
ALTER TABLE ending_guide ADD CONSTRAINT ending_guide_description_en_check CHECK (char_length(description_en) <= 5000);
