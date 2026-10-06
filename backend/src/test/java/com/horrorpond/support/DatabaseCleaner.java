package com.horrorpond.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 커밋하는 통합 테스트용. 컨테이너를 공유하므로 다른 테스트에 데이터를 남기지 않게 비운다.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("""
                TRUNCATE game_genre, game_developer, game_media, store_link, curation_article,
                         game, developer, genre, steam_app_seed, steam_raw_snapshot, ingestion_job, shedlock
                RESTART IDENTITY CASCADE""");
    }
}
