package com.horrorpond.curation.api;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.repository.DeveloperRepository;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.CurationArticleRepository;
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
    void onlyPublishedGameWithPublishedArticleIsExposed() throws Exception {
        create(spec("visible"));
        create(spec("candidate").gameStatus(GameStatus.CANDIDATE));
        create(spec("hidden").gameStatus(GameStatus.HIDDEN));
        create(spec("draft-article").articleStatus(ArticleStatus.DRAFT));
        create(spec("no-article").articleStatus(null));

        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug", contains("visible")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/games/visible")).andExpect(status().isOk());
        for (String slug : List.of("candidate", "hidden", "draft-article", "no-article", "missing")) {
            mvc.perform(get("/api/games/" + slug))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
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
    void sortByLatestPublishedAndByReleaseDateWithNullsLast() throws Exception {
        create(spec("old-release").publishedAt(T.plusSeconds(300)).releaseDate(LocalDate.of(2010, 1, 1)));
        create(spec("new-release").publishedAt(T.plusSeconds(100)).releaseDate(LocalDate.of(2024, 1, 1)));
        create(spec("no-release").publishedAt(T.plusSeconds(200)).releaseDate(null));

        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[*].slug", contains("old-release", "no-release", "new-release")));
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
        mvc.perform(get("/api/games").param("sort", "POPULAR")).andExpect(status().isBadRequest());
    }

    @Test
    void listOf24UsesAtMostThreeQueries() throws Exception {
        IntStream.range(0, 30).forEach(i ->
                create(spec("game-" + i).publishedAt(T.plusSeconds(i)).genres("psychological", "occult")));
        Statistics stats = statistics();
        stats.clear();

        mvc.perform(get("/api/games").param("size", "24"))
                .andExpect(jsonPath("$.content", hasSize(24)))
                .andExpect(jsonPath("$.content[23].genres", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(30));

        // 목록 1 + 장르 IN 1 + count 1
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
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
                    spec.releaseDate == null ? "Coming soon" : spec.releaseDate.toString(), false, spec.coop,
                    media, credits));
            game.replaceGenres(new HashSet<>(spec.genreSlugs.stream()
                    .map(slug -> genreRepository.findBySlug(slug).orElseThrow())
                    .toList()));
            gameRepository.save(game);

            if (spec.articleStatus != null) {
                CurationArticle article = CurationArticle.draft(game.getId(), "Article " + spec.slug,
                        "One liner " + spec.slug, "# Why\n\n**Markdown** body", List.of("h1", "h2", "h3", "h4"));
                if (spec.sponsored) {
                    article.markSponsored("Key provided by the publisher");
                }
                if (spec.articleStatus == ArticleStatus.PUBLISHED) {
                    article.publish(spec.publishedAt);
                }
                articleRepository.save(article);
            }
            if (spec.gameStatus != GameStatus.CANDIDATE) {
                game.publish(true, spec.publishedAt);
            }
            if (spec.gameStatus == GameStatus.HIDDEN) {
                game.hide();
            }
        });
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
    }
}
