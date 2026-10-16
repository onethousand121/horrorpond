package com.horrorpond.curation.api;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.domain.StorePrice;
import com.horrorpond.catalog.domain.TrendingOverride;
import com.horrorpond.catalog.repository.DeveloperRepository;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.curation.domain.ArticleKind;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.domain.PlayVideo;
import com.horrorpond.curation.repository.CurationArticleRepository;
import com.horrorpond.curation.repository.PlayVideoRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicGameApiTest {

    private static final Instant T = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GenreRepository genreRepository;

    @Autowired
    DeveloperRepository developerRepository;

    @Autowired
    CurationArticleRepository articleRepository;

    @Autowired
    PlayVideoRepository playVideoRepository;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory emf;

    private int nextAppid = 1000;

    @BeforeEach
    @AfterEach
    void clean() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void genresAreListedInDisplayOrder() throws Exception {
        mvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("psychological", "survival", "occult", "analog", "cosmic")))
                .andExpect(jsonPath("$[0].name").value("심리 공포"))
                .andExpect(jsonPath("$[0].description").value("불안과 긴장, 정신적 압박이 중심인 공포"));
    }

    @Test
    void exposureRules() throws Exception {
        create(spec("pinned").articleStatus(null));                                     // 공개(글 없음)
        create(spec("pinned-adult").articleStatus(null).adult(true));                   // 공개는 성인이어도 노출
        create(spec("auto-reviewed").gameStatus(GameStatus.CANDIDATE).reviewCount(10)); // 자동: 리뷰 기준 충족
        create(spec("auto-upcoming").gameStatus(GameStatus.CANDIDATE).comingSoon(true)); // 자동: 출시 예정
        create(spec("auto-few").gameStatus(GameStatus.CANDIDATE).reviewCount(9));
        create(spec("auto-adult").gameStatus(GameStatus.CANDIDATE).reviewCount(5000).adult(true));
        create(spec("hidden").gameStatus(GameStatus.HIDDEN).reviewCount(5000));

        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug",
                        containsInAnyOrder("pinned", "pinned-adult", "auto-reviewed", "auto-upcoming")))
                .andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get("/api/games/pinned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article").doesNotExist());
        mvc.perform(get("/api/games/auto-reviewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(10));
        for (String slug : List.of("auto-few", "auto-adult", "hidden", "missing")) {
            mvc.perform(get("/api/games/" + slug))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Test
    void pickedFilterReturnsOnlyGamesWithPublishedArticle() throws Exception {
        create(spec("picked").publishedAt(T.plusSeconds(10)));
        create(spec("picked-later").publishedAt(T.plusSeconds(20)));
        create(spec("draft").articleStatus(ArticleStatus.DRAFT));
        create(spec("no-article").articleStatus(null));

        mvc.perform(get("/api/games").param("picked", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("picked-later", "picked")))
                .andExpect(jsonPath("$.content[0].picked").value(true))
                .andExpect(jsonPath("$.content[0].oneLiner").value("One liner picked-later"));
        mvc.perform(get("/api/games/draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article").doesNotExist());
        mvc.perform(get("/api/games").param("size", "48"))
                .andExpect(jsonPath("$.content[?(@.slug == 'no-article')].picked", contains(false)))
                .andExpect(jsonPath("$.content[?(@.slug == 'no-article')].highlights[*]", hasSize(0)));
    }

    @Test
    void genreFilterMatchesSteamTagsToo() throws Exception {
        create(spec("by-tag").gameStatus(GameStatus.CANDIDATE).reviewCount(100)
                .tags("Horror", "Psychological Horror", "Atmospheric"));
        create(spec("unrelated-tag").gameStatus(GameStatus.CANDIDATE).reviewCount(100).tags("Horror", "Zombies"));

        mvc.perform(get("/api/games").param("genre", "psychological"))
                .andExpect(jsonPath("$.content[*].slug", contains("by-tag")))
                .andExpect(jsonPath("$.content[0].genres[*].slug", contains("psychological")));
        mvc.perform(get("/api/games/by-tag"))
                .andExpect(jsonPath("$.genres[*].slug", contains("psychological")));
    }

    @Test
    void pinnedAdultGameIsListedWithAdultFlag() throws Exception {
        create(spec("pinned-adult").adult(true).reviewCount(5000));
        create(spec("auto-adult").gameStatus(GameStatus.CANDIDATE).adult(true).reviewCount(5000));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[*].slug", contains("pinned-adult")))
                .andExpect(jsonPath("$.content[0].adult").value(true));
        mvc.perform(get("/api/games/pinned-adult"))
                .andExpect(jsonPath("$.adult").value(true));
    }

    @Test
    void adultDetailHasOnlyBasicsAndStoreLinks() throws Exception {
        create(spec("pinned-adult").adult(true).developerCount(1));
        jdbc.update("""
                INSERT INTO play_video (game_id, youtube_id, title, sort_order, kind, channel_name)
                SELECT id, 'dQw4w9WgXcQ', 'Keeper', 0, 'KEEPER', NULL FROM game WHERE slug = 'pinned-adult'
                UNION ALL
                SELECT id, 'aaaaaaaaaaa', 'Other', 1, 'REFERENCE', 'Channel' FROM game WHERE slug = 'pinned-adult'""");
        jdbc.update("""
                INSERT INTO achievement_guide (game_id, name, sort_order)
                SELECT id, 'Ach', 0 FROM game WHERE slug = 'pinned-adult'""");
        jdbc.update("UPDATE game SET ending_type = 'MULTIPLE' WHERE slug = 'pinned-adult'");
        jdbc.update("""
                INSERT INTO ending_guide (game_id, name, sort_order)
                SELECT id, 'End', 0 FROM game WHERE slug = 'pinned-adult'""");
        jdbc.update("""
                INSERT INTO korean_patch (game_id, url, sort_order)
                SELECT id, 'https://example.com/patch', 0 FROM game WHERE slug = 'pinned-adult'""");

        // 성인 게임은 API에서부터 소개·미디어·글·영상·공략을 내려주지 않는다
        mvc.perform(get("/api/games/pinned-adult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title pinned-adult"))
                .andExpect(jsonPath("$.adult").value(true))
                .andExpect(jsonPath("$.shortDescription").doesNotExist())
                .andExpect(jsonPath("$.media", hasSize(0)))
                .andExpect(jsonPath("$.article").doesNotExist())
                .andExpect(jsonPath("$.playVideos", hasSize(0)))
                .andExpect(jsonPath("$.referenceVideos", hasSize(0)))
                .andExpect(jsonPath("$.achievements", hasSize(0)))
                .andExpect(jsonPath("$.endingType").doesNotExist())
                .andExpect(jsonPath("$.endings", hasSize(0)))
                .andExpect(jsonPath("$.koreanPatches", hasSize(0)))
                .andExpect(jsonPath("$.storeLinks[0].store").value("STEAM"))
                .andExpect(jsonPath("$.developers", hasSize(1)))
                .andExpect(jsonPath("$.releaseDate").value("2020-01-01"));
    }

    @Test
    void hubStatsCountOnlyVisibleGames() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        create(spec("today").releaseDate(today));
        // 출시일이 오늘인데 아직 안 풀린 게임도 "오늘 출시"로 센다 (출시일 기준)
        create(spec("today-locked").comingSoon(true).releaseDate(today));
        create(spec("yesterday").releaseDate(today.minusDays(1)));
        create(spec("tomorrow").comingSoon(true).releaseDate(today.plusDays(1)));
        create(spec("tomorrow-2").comingSoon(true).releaseDate(today.plusDays(1)));
        create(spec("in-two-days").comingSoon(true).releaseDate(today.plusDays(2)));
        create(spec("hidden-today").gameStatus(GameStatus.HIDDEN).releaseDate(today));

        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releasedToday").value(2))
                .andExpect(jsonPath("$.releasingTomorrow").value(2));
        mvc.perform(get("/api/games").param("release", "TODAY"))
                .andExpect(jsonPath("$.content[*].slug", containsInAnyOrder("today", "today-locked")));
        mvc.perform(get("/api/games").param("release", "TOMORROW"))
                .andExpect(jsonPath("$.content[*].slug", containsInAnyOrder("tomorrow", "tomorrow-2")));
    }

    @Test
    void koreanFilterAndLanguagesInResponses() throws Exception {
        create(spec("korean").reviewCount(300).languages(List.of("en", "ko"), List.of("en")));
        create(spec("english-only").reviewCount(200).languages(List.of("en", "ja"), List.of("ja")));

        mvc.perform(get("/api/games").param("korean", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("korean")))
                .andExpect(jsonPath("$.content[0].languages", contains("en", "ko")))
                .andExpect(jsonPath("$.content[0].audioLanguages", contains("en")))
                .andExpect(jsonPath("$.content[0].koreanPatch").value(false));
        mvc.perform(get("/api/games").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[*].slug", contains("korean", "english-only")));

        // 공식 한국어가 없어도 유저 한글패치가 있으면 한국어 필터에 들어간다
        jdbc.update("""
                INSERT INTO korean_patch (game_id, url, sort_order)
                SELECT id, 'https://example.com/patch', 0 FROM game WHERE slug = 'english-only'""");
        mvc.perform(get("/api/games").param("korean", "true").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[*].slug", contains("korean", "english-only")))
                .andExpect(jsonPath("$.content[1].koreanPatch").value(true))
                .andExpect(jsonPath("$.content[1].languages", contains("en", "ja")));
        mvc.perform(get("/api/games/english-only"))
                .andExpect(jsonPath("$.languages", contains("en", "ja")))
                .andExpect(jsonPath("$.audioLanguages", contains("ja")));
    }

    @Test
    void trendingRanksHorrorGamesByReviewsGainedThisWeek() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        LocalDate old = today.minusDays(60);
        create(spec("rising").releaseDate(old).reviewCount(1500).tags("Horror", "Indie"));
        metric("rising", today.minusDays(10), 1000);                          // +500
        create(spec("cooled").releaseDate(old).reviewCount(3000).tags("Psychological Horror"));
        metric("cooled", today.minusDays(8), 2990);                           // +10 (출시 초반엔 컸지만 지금은 식음)
        metric("cooled", today.minusDays(30), 100);                           // 14일보다 오래된 기록은 안 본다
        create(spec("new-hit").releaseDate(today.minusDays(3)).reviewCount(300)); // 출시 7일 이내: 0에서 셈, 태그 없음
        create(spec("no-history").releaseDate(old).reviewCount(5000).tags("Horror"));     // 기준 기록 없음 → 제외
        create(spec("not-horror").releaseDate(old).reviewCount(2000)
                .tags("Action", "Roguelike", "Indie", "Pixel Graphics", "2D", "Horror")); // Horror가 6위 → 제외
        metric("not-horror", today.minusDays(9), 1200);
        create(spec("excluded").releaseDate(old).reviewCount(2000).tags("Horror")
                .trendingOverride(TrendingOverride.EXCLUDED));
        metric("excluded", today.minusDays(9), 1000);
        create(spec("pinned").releaseDate(old).reviewCount(5).tags("Comedy")
                .trendingOverride(TrendingOverride.PINNED));                  // 기준과 상관없이 맨 앞
        create(spec("upcoming").comingSoon(true).releaseDate(today.plusDays(3)));

        mvc.perform(get("/api/games").param("sort", "TRENDING"))
                .andExpect(jsonPath("$.content[*].slug", contains("pinned", "rising", "new-hit", "cooled")));
        mvc.perform(get("/api/games").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[0].slug").value("no-history"));
    }

    @Test
    void pinnedTrendingGamesFollowTheCuratorOrder() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        create(spec("pin-a").releaseDate(today.minusDays(60)).reviewCount(10)
                .trendingOverride(TrendingOverride.PINNED).pinOrder(1));
        create(spec("pin-b").releaseDate(today.minusDays(60)).reviewCount(10)
                .trendingOverride(TrendingOverride.PINNED).pinOrder(0));
        create(spec("pin-unordered").releaseDate(today.minusDays(60)).reviewCount(10)
                .trendingOverride(TrendingOverride.PINNED));
        create(spec("rising").releaseDate(today.minusDays(60)).reviewCount(1500).tags("Horror"));
        metric("rising", today.minusDays(10), 1000);

        // 순서를 정한 고정 게임 → 순서 없는 고정 게임 → 리뷰 증가량 순
        mvc.perform(get("/api/games").param("sort", "TRENDING"))
                .andExpect(jsonPath("$.content[*].slug", contains("pin-b", "pin-a", "pin-unordered", "rising")));
    }

    @Test
    void freeFilterAndPrices() throws Exception {
        create(spec("free-game").price(new StorePrice(true, null, null)));
        create(spec("paid-game").price(new StorePrice(false, 2050000, "KRW")));
        create(spec("unknown-price"));
        // 출시 전 게임은 가격·무료 표시가 없으니 무료 목록에도 없다
        create(spec("free-upcoming").releaseDate(null).comingSoon(true).price(new StorePrice(true, null, null)));

        mvc.perform(get("/api/games").param("free", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("free-game")))
                .andExpect(jsonPath("$.content[0].free").value(true));
        mvc.perform(get("/api/games/paid-game"))
                .andExpect(jsonPath("$.free").value(false))
                .andExpect(jsonPath("$.priceAmount").value(2050000))
                .andExpect(jsonPath("$.priceCurrency").value("KRW"));
        mvc.perform(get("/api/games/free-game"))
                .andExpect(jsonPath("$.free").value(true))
                .andExpect(jsonPath("$.priceAmount").doesNotExist());
    }

    @Test
    void newReleasesWithoutReviewsAreVisibleForTenDays() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        create(spec("day-ten").gameStatus(GameStatus.CANDIDATE).releaseDate(today.minusDays(10)));
        create(spec("day-eleven").gameStatus(GameStatus.CANDIDATE).releaseDate(today.minusDays(11)));
        create(spec("day-eleven-reviewed").gameStatus(GameStatus.CANDIDATE).releaseDate(today.minusDays(11))
                .reviewCount(10));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[*].slug", containsInAnyOrder("day-ten", "day-eleven-reviewed")));
        mvc.perform(get("/api/games/day-ten")).andExpect(status().isOk());
        mvc.perform(get("/api/games/day-eleven")).andExpect(status().isNotFound());
    }

    @Test
    void releaseWindowsAndPopularSort() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        create(spec("soon").comingSoon(true).releaseDate(today.plusDays(30)));
        create(spec("sooner").comingSoon(true).releaseDate(today.plusDays(5)));
        create(spec("recent").releaseDate(today.minusDays(10)).reviewCount(50));
        // 출시 예정으로 수집됐지만 출시일이 지난 게임 (다음 갱신 전)
        create(spec("released-stale").comingSoon(true).releaseDate(today.minusDays(1)));
        // 출시일이 오늘이고 Steam에서 아직 안 풀림 → 출시 예정 / 오늘 풀림 → 최근 출시
        create(spec("today-locked").comingSoon(true).releaseDate(today));
        create(spec("today-released").releaseDate(today));
        create(spec("older").releaseDate(today.minusDays(200)).reviewCount(9000));

        mvc.perform(get("/api/games").param("release", "UPCOMING"))
                .andExpect(jsonPath("$.content[*].slug", contains("today-locked", "sooner", "soon")));
        mvc.perform(get("/api/games").param("release", "RECENT"))
                .andExpect(jsonPath("$.content[*].slug",
                        containsInAnyOrder("recent", "released-stale", "today-released")));
        mvc.perform(get("/api/games").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[0].slug").value("older"))
                .andExpect(jsonPath("$.content[1].slug").value("recent"));
    }

    @Test
    void searchMatchesTitleCaseInsensitivelyAndEscapesWildcards() throws Exception {
        create(spec("outlast"));
        create(spec("outlast-trials"));
        create(spec("phasmo"));
        create(spec("hidden-outlast").gameStatus(GameStatus.HIDDEN));

        mvc.perform(get("/api/games").param("q", " OUTLAST "))
                .andExpect(jsonPath("$.content[*].slug", containsInAnyOrder("outlast", "outlast-trials")));
        // 제목은 "Title <slug>". % 와 _ 는 와일드카드가 아니라 글자 그대로 찾는다
        mvc.perform(get("/api/games").param("q", "%"))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/games").param("q", "title_"))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/games").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        // 주소(slug) 모양으로도 찾는다: 공백은 - 로 본다
        mvc.perform(get("/api/games").param("q", "outlast trials"))
                .andExpect(jsonPath("$.content[*].slug", contains("outlast-trials")));
        mvc.perform(get("/api/games").param("q", "outlast-trials"))
                .andExpect(jsonPath("$.content[*].slug", contains("outlast-trials")));
    }

    @Test
    void englishFallsBackToKoreanAndSearchMatchesBothTitles() throws Exception {
        create(spec("translated").englishTitle("Silent Pond").genres("psychological"));
        create(spec("korean-only").genres("psychological"));

        mvc.perform(get("/api/games").param("lang", "en").param("sort", "RELEASE"))
                .andExpect(jsonPath("$.content[?(@.slug == 'translated')].title", contains("Silent Pond")))
                .andExpect(jsonPath("$.content[?(@.slug == 'translated')].shortDescription",
                        contains("English description translated")))
                .andExpect(jsonPath("$.content[?(@.slug == 'translated')].releaseDateText", contains("Oct 1, 2026")))
                .andExpect(jsonPath("$.content[?(@.slug == 'korean-only')].title", contains("Title korean-only")))
                .andExpect(jsonPath("$.content[0].genres[0].name").value("Psychological"));
        mvc.perform(get("/api/games").param("lang", "ko"))
                .andExpect(jsonPath("$.content[?(@.slug == 'translated')].title", contains("Title translated")))
                .andExpect(jsonPath("$.content[0].genres[0].name").value("심리 공포"));
        mvc.perform(get("/api/games/translated").param("lang", "en"))
                .andExpect(jsonPath("$.title").value("Silent Pond"))
                .andExpect(jsonPath("$.genres[0].name").value("Psychological"));
        mvc.perform(get("/api/genres").param("lang", "en"))
                .andExpect(jsonPath("$[0].name").value("Psychological"));
        // 검색은 언어와 상관없이 한국어·영어 제목 모두에서
        mvc.perform(get("/api/games").param("q", "silent"))
                .andExpect(jsonPath("$.content[*].slug", contains("translated")));
        mvc.perform(get("/api/games").param("lang", "fr"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summaryHasDescriptionAndTopFiveTags() throws Exception {
        create(spec("tagged").tags("Horror", "Co-op", "Psychological Horror", "Dark", "Atmospheric", "Indie"));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[0].shortDescription").value("Description tagged"))
                .andExpect(jsonPath("$.content[0].tags[*]",
                        contains("Horror", "Co-op", "Psychological Horror", "Dark", "Atmospheric")));
    }

    @Test
    void genreFilterUsesExistsWithoutDuplicates() throws Exception {
        create(spec("a").genres("psychological"));
        create(spec("b").genres("psychological", "survival", "occult"));
        create(spec("c").genres("occult"));

        mvc.perform(get("/api/games").param("genre", "psychological"))
                .andExpect(jsonPath("$.content[*].slug", contains("b", "a")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].genres[*].slug", contains("psychological", "survival", "occult")));
        mvc.perform(get("/api/games").param("genre", "no-such-genre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void coopFilter() throws Exception {
        create(spec("solo"));
        create(spec("together").coop(true));

        mvc.perform(get("/api/games").param("coop", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("together")))
                .andExpect(jsonPath("$.content[0].coop").value(true));
        mvc.perform(get("/api/games").param("coop", "false"))
                .andExpect(jsonPath("$.content[*].slug", contains("solo")));
        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void sortByLatestAddedAndByReleaseDateWithNullsLast() throws Exception {
        // LATEST: 사이트에 올라온(수집된) 순서. 만든 순서의 역순
        create(spec("old-release").releaseDate(LocalDate.of(2010, 1, 1)));
        create(spec("new-release").releaseDate(LocalDate.of(2024, 1, 1)));
        create(spec("no-release").releaseDate(null));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[*].slug", contains("no-release", "new-release", "old-release")));
        mvc.perform(get("/api/games").param("sort", "RELEASE"))
                .andExpect(jsonPath("$.content[*].slug", contains("new-release", "old-release", "no-release")));
    }

    @Test
    void paging() throws Exception {
        IntStream.range(0, 5).forEach(i -> create(spec("game-" + i).publishedAt(T.plusSeconds(i))));

        mvc.perform(get("/api/games").param("size", "2"))
                .andExpect(jsonPath("$.content[*].slug", contains("game-4", "game-3")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/games").param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.content[*].slug", contains("game-0")))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void sizeAboveMaximumIs400() throws Exception {
        mvc.perform(get("/api/games").param("size", "49"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/games").param("size", "48")).andExpect(status().isOk());
        mvc.perform(get("/api/games").param("sort", "NOPE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/games").param("release", "NOPE")).andExpect(status().isBadRequest());
    }

    @Test
    void listOf24UsesAtMostFourQueries() throws Exception {
        IntStream.range(0, 30).forEach(i ->
                create(spec("game-" + i).publishedAt(T.plusSeconds(i)).genres("psychological", "occult")));
        Statistics stats = statistics();
        stats.clear();

        mvc.perform(get("/api/games").param("size", "24"))
                .andExpect(jsonPath("$.content", hasSize(24)))
                .andExpect(jsonPath("$.content[23].genres", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(30));

        // 전체 장르(태그 매핑용) 1 + 목록 1 + 큐레이터 장르 IN 1 + count 1
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    void detailQueryCountDoesNotDependOnMediaOrDeveloperCount() throws Exception {
        create(spec("small").mediaCount(1).developerCount(1).genres("occult"));
        create(spec("large").mediaCount(12).developerCount(5).genres("psychological", "survival", "occult"));
        Statistics stats = statistics();

        stats.clear();
        mvc.perform(get("/api/games/small"))
                .andExpect(jsonPath("$.media", hasSize(1)))
                .andExpect(jsonPath("$.developers", hasSize(1)));
        long small = stats.getPrepareStatementCount();

        stats.clear();
        mvc.perform(get("/api/games/large"))
                .andExpect(jsonPath("$.media", hasSize(12)))
                .andExpect(jsonPath("$.developers", hasSize(5)))
                .andExpect(jsonPath("$.genres", hasSize(3)));
        long large = stats.getPrepareStatementCount();

        assertThat(large).isEqualTo(small);
    }

    @Test
    void detailContainsArticleAndSortedCollections() throws Exception {
        create(spec("full").mediaCount(3).developerCount(2).genres("survival", "psychological"));

        mvc.perform(get("/api/games/full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title full"))
                .andExpect(jsonPath("$.genres[*].slug", contains("psychological", "survival")))
                .andExpect(jsonPath("$.media[*].url", contains("https://media/full/0", "https://media/full/1",
                        "https://media/full/2")))
                .andExpect(jsonPath("$.developers[0].role").value("DEVELOPER"))
                .andExpect(jsonPath("$.storeLinks[0].store").value("STEAM"))
                .andExpect(jsonPath("$.article.body").value("# Why\n\n**Markdown** body"))
                .andExpect(jsonPath("$.article.highlights", hasSize(4)))
                .andExpect(jsonPath("$.article.sponsored").value(false))
                .andExpect(jsonPath("$.article.publishedAt").value("2026-01-01T00:00:00Z"));
        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[0].highlights", hasSize(3)))
                .andExpect(jsonPath("$.content[0].oneLiner").value("One liner full"));
    }

    @Test
    void sponsoredGameShowsFlagInListAndDisclosureInDetail() throws Exception {
        create(spec("ad").sponsored(true));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[0].sponsored").value(true));
        mvc.perform(get("/api/games/ad"))
                .andExpect(jsonPath("$.article.sponsored").value(true))
                .andExpect(jsonPath("$.article.sponsorDisclosure").value("Key provided by the publisher"));
    }

    // ===== fixtures =====

    @Test
    void keeperListPutsPicksThenReviewsThenVideoOnlyGames() throws Exception {
        create(spec("video-only").articleStatus(null).keeperVideo(true).reviewCount(5000));
        create(spec("review").articleKind(ArticleKind.REVIEW).reviewCount(1000));
        create(spec("pick").reviewCount(10));
        create(spec("nothing").articleStatus(null).reviewCount(9000));

        // 리뷰 수와 상관없이 추천 → 후기 → 영상만 있는 게임 순이고, 아무것도 없는 게임은 빠진다
        mvc.perform(get("/api/games").param("keeper", "true").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[*].slug", contains("pick", "review", "video-only")));
        // 다른 목록은 원래 정렬 그대로
        mvc.perform(get("/api/games").param("sort", "POPULAR"))
                .andExpect(jsonPath("$.content[*].slug", contains("nothing", "video-only", "review", "pick")));
    }

    private GameSpec spec(String slug) {
        return new GameSpec(slug);
    }

    private void create(GameSpec spec) {
        int appid = nextAppid++;
        tx.executeWithoutResult(status -> {
            List<SteamGameData.Credit> credits = new ArrayList<>();
            for (int i = 0; i < spec.developerCount; i++) {
                Developer developer = developerRepository.save(
                        Developer.create("Dev " + spec.slug + " " + i, "dev-" + spec.slug + "-" + i));
                credits.add(new SteamGameData.Credit(developer, i == 0 ? DeveloperRole.DEVELOPER : DeveloperRole.PUBLISHER));
            }
            List<SteamGameData.Media> media = IntStream.range(0, spec.mediaCount)
                    .mapToObj(i -> new SteamGameData.Media(MediaType.SCREENSHOT, "https://media/" + spec.slug + "/" + i,
                            "https://thumb/" + spec.slug + "/" + i))
                    .toList();
            Game game = Game.candidateFromSteam(appid, "Title " + spec.slug, spec.slug);
            game.applySteamData(new SteamGameData("Title " + spec.slug, "Description " + spec.slug,
                    "https://img/" + spec.slug + ".jpg", spec.releaseDate,
                    spec.releaseDate == null ? "Coming soon" : spec.releaseDate.toString(), spec.comingSoon, spec.coop,
                    spec.reviewCount, spec.adult, media, credits));
            game.applySteamTags(spec.tags);
            game.applySteamLanguages(spec.languages, spec.audioLanguages);
            if (spec.englishTitle != null) {
                game.applyEnglishText(spec.englishTitle, "English description " + spec.slug, "Oct 1, 2026");
            }
            game.replaceGenres(new HashSet<>(spec.genreSlugs.stream()
                    .map(slug -> genreRepository.findBySlug(slug).orElseThrow())
                    .toList()));
            game.changeTrendingOverride(spec.trendingOverride);
            game.changeTrendingPinOrder(spec.pinOrder);
            game.applyPrice(spec.price);
            gameRepository.save(game);

            if (spec.articleStatus != null) {
                CurationArticle article = CurationArticle.draft(game.getId(), spec.articleKind, "Article " + spec.slug,
                        "One liner " + spec.slug, "# Why\n\n**Markdown** body", List.of("h1", "h2", "h3", "h4"));
                if (spec.sponsored) {
                    article.markSponsored("Key provided by the publisher");
                }
                if (spec.articleStatus == ArticleStatus.PUBLISHED) {
                    article.publish(spec.publishedAt);
                }
                articleRepository.save(article);
            }
            if (spec.keeperVideo) {
                playVideoRepository.save(PlayVideo.create(game.getId(), "https://youtu.be/dQw4w9WgXcQ", null, 0));
            }
            if (spec.gameStatus != GameStatus.CANDIDATE) {
                game.publish(spec.publishedAt);
            }
            if (spec.gameStatus == GameStatus.HIDDEN) {
                game.hide();
            }
        });
    }

    /** 그날의 리뷰 수 기록 (트렌드 기준점) */
    private void metric(String slug, LocalDate capturedOn, int reviewCount) {
        jdbc.update("""
                INSERT INTO game_metric_daily (game_id, store, captured_on, review_count)
                SELECT id, 'STEAM', ?, ? FROM game WHERE slug = ?""", capturedOn, reviewCount, slug);
    }

    private Statistics statistics() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        assertThat(stats.isStatisticsEnabled()).isTrue();
        return stats;
    }

    private static final class GameSpec {

        private final String slug;
        private GameStatus gameStatus = GameStatus.PUBLISHED;
        private ArticleStatus articleStatus = ArticleStatus.PUBLISHED;
        private Instant publishedAt = T;
        private LocalDate releaseDate = LocalDate.of(2020, 1, 1);
        private boolean coop;
        private boolean sponsored;
        private List<String> genreSlugs = List.of();
        private int mediaCount = 1;
        private int developerCount = 1;
        private Integer reviewCount;
        private boolean adult;
        private boolean comingSoon;
        private List<String> tags = List.of();
        private List<String> languages = List.of();
        private List<String> audioLanguages = List.of();
        private String englishTitle;
        private TrendingOverride trendingOverride;
        private Integer pinOrder;
        private StorePrice price = StorePrice.UNKNOWN;
        private ArticleKind articleKind = ArticleKind.PICK;
        private boolean keeperVideo;

        GameSpec(String slug) {
            this.slug = slug;
        }

        GameSpec gameStatus(GameStatus value) { gameStatus = value; return this; }

        GameSpec articleStatus(ArticleStatus value) { articleStatus = value; return this; }

        GameSpec publishedAt(Instant value) { publishedAt = value; return this; }

        GameSpec releaseDate(LocalDate value) { releaseDate = value; return this; }

        GameSpec coop(boolean value) { coop = value; return this; }

        GameSpec sponsored(boolean value) { sponsored = value; return this; }

        GameSpec genres(String... slugs) { genreSlugs = List.of(slugs); return this; }

        GameSpec mediaCount(int value) { mediaCount = value; return this; }

        GameSpec developerCount(int value) { developerCount = value; return this; }

        GameSpec reviewCount(Integer value) { reviewCount = value; return this; }

        GameSpec adult(boolean value) { adult = value; return this; }

        GameSpec comingSoon(boolean value) { comingSoon = value; return this; }

        GameSpec tags(String... values) { tags = List.of(values); return this; }
        GameSpec languages(List<String> all, List<String> audio) { languages = all; audioLanguages = audio; return this; }

        GameSpec englishTitle(String value) { englishTitle = value; return this; }

        GameSpec trendingOverride(TrendingOverride value) { trendingOverride = value; return this; }

        GameSpec pinOrder(Integer value) { pinOrder = value; return this; }

        GameSpec price(StorePrice value) { price = value; return this; }

        GameSpec articleKind(ArticleKind value) { articleKind = value; return this; }

        GameSpec keeperVideo(boolean value) { keeperVideo = value; return this; }
    }
}
