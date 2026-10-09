package com.horrorpond.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 커밋하는 통합 테스트용. 컨테이너를 공유하므로 다른 테스트에 데이터를 남기지 않게 비운다.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    /**
     * V3에서 시드한 장르는 마이그레이션 데이터이므로 남긴다.
     */
    static final String SEEDED_GENRE_SLUGS = "'psychological', 'survival', 'occult', 'analog', 'cosmic'";

    /**
     * shedlock 행은 지우지 않고 락만 푼다. ShedLock은 한 번 만든 락 행을 기억해 이후 UPDATE만 하므로,
     * 행을 지우면 같은 컨텍스트에서 다시는 락을 잡지 못한다.
     */
    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("""
                TRUNCATE game_genre, game_developer, game_media, store_link, curation_article,
                         play_video, achievement_guide, game_metric_daily,
                         game, developer, steam_app_seed, itch_game_seed, steam_raw_snapshot, ingestion_job
                RESTART IDENTITY CASCADE""");
        jdbc.update("DELETE FROM genre WHERE slug NOT IN (" + SEEDED_GENRE_SLUGS + ")");
        jdbc.update("UPDATE shedlock SET lock_until = locked_at");
    }
}
