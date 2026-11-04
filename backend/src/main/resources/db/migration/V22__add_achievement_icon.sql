-- 업적 아이콘 (Steam 커뮤니티 CDN 주소). 없으면 null. 추가만 하므로 직전 버전 애플리케이션과 호환된다
ALTER TABLE achievement_guide ADD COLUMN icon_url VARCHAR(500);
