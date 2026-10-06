package com.horrorpond.curation.api;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.SteamGameData;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ADMIN_API_KEY=" + AdminCurationApiTest.KEY)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminCurationApiTest {

    static final String KEY = "curation-test-key";

    private static final String ARTICLE = """
            {"title":"보이지 않는 것을 쫓는 밤","oneLiner":"4인 협동 유령 조사",
             "body":"## 왜 추천하나\\n\\n**증거**를 모으는 긴장감","highlights":["4인 협동","음성 인식","장비 운용"],
             "sponsored":false,"sponsorDisclosure":null}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void reviewQueueToPublishedPublicGame() throws Exception {
        Long id = steamCandidate(739630, "Phasmophobia", "phasmophobia-739630");
        steamCandidate(1262350, "SIGNALIS", "signalis-1262350");

        // 1) 검토 대기열 (제목 부분일치, 대소문자 무시)
        mvc.perform(admin(get("/api/admin/games").param("status", "CANDIDATE").param("q", "PHASMO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].source").value("STEAM"))
                .andExpect(jsonPath("$.content[0].externalId").value("739630"))
                .andExpect(jsonPath("$.content[0].hasArticle").value(false))
                .andExpect(jsonPath("$.content[0].articleStatus").doesNotExist());

        // 2) 큐레이션 (slug, 장르)
        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"phasmophobia\",\"genreSlugs\":[\"psychological\",\"occult\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("phasmophobia"));

        // 3) 글 작성 (초안)
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON).content(ARTICLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.highlights", hasSize(3)));
        mvc.perform(get("/api/games/phasmophobia")).andExpect(status().isNotFound());

        // 4) 공개
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.articleStatus").value("PUBLISHED"));

        // 5) 공개 API에 노출
        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[*].slug", contains("phasmophobia")))
                .andExpect(jsonPath("$.content[0].genres[*].slug", contains("psychological", "occult")))
                .andExpect(jsonPath("$.content[0].oneLiner").value("4인 협동 유령 조사"));
        mvc.perform(get("/api/games/phasmophobia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.title").value("보이지 않는 것을 쫓는 밤"));

        // 6) 숨김 → 공개 API에서 사라짐, 숨김 해제 → CANDIDATE
        mvc.perform(admin(post("/api/admin/games/{id}/hide", id)))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mvc.perform(get("/api/games/phasmophobia")).andExpect(status().isNotFound());
        mvc.perform(admin(post("/api/admin/games/{id}/unhide", id)))
                .andExpect(jsonPath("$.status").value("CANDIDATE"));
    }

    @Test
    void detailIncludesCuratorFieldsAndArticle() throws Exception {
        Long id = steamCandidate(739630, "Phasmophobia", "phasmophobia-739630");

        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Phasmophobia"))
                .andExpect(jsonPath("$.shortDescription").value("desc"))
                .andExpect(jsonPath("$.headerImageUrl").value("https://img/739630"))
                .andExpect(jsonPath("$.steamUrl").value("https://store.steampowered.com/app/739630"))
                .andExpect(jsonPath("$.genreSlugs", hasSize(0)))
                .andExpect(jsonPath("$.article").doesNotExist());

        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"phasmophobia\",\"genreSlugs\":[\"psychological\",\"occult\"]}"))
                .andExpect(status().isOk());
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON).content(ARTICLE))
                .andExpect(status().isOk());

        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.slug").value("phasmophobia"))
                .andExpect(jsonPath("$.genreSlugs", contains("occult", "psychological")))
                .andExpect(jsonPath("$.article.status").value("DRAFT"))
                .andExpect(jsonPath("$.article.highlights", contains("4인 협동", "음성 인식", "장비 운용")));
        mvc.perform(admin(get("/api/admin/games/{id}", 9999))).andExpect(status().isNotFound());
    }

    @Test
    void listCountsOtherGamesWithSameTitle() throws Exception {
        steamCandidate(9050, "DOOM 3", "doom-3-9050");
        steamCandidate(208200, "Doom 3", "doom-3-208200");
        steamCandidate(10, "Unique", "unique-10");

        mvc.perform(admin(get("/api/admin/games").param("q", "doom")))
                .andExpect(jsonPath("$.content[*].sameTitleCount", contains(1, 1)));
        mvc.perform(admin(get("/api/admin/games").param("q", "unique")))
                .andExpect(jsonPath("$.content[0].sameTitleCount").value(0));
    }

    @Test
    void publishWithoutArticleIs409() throws Exception {
        Long id = steamCandidate(1, "No Article", "no-article-1");

        mvc.perform(admin(post("/api/admin/games/{id}/publish", id)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void publishWithoutHighlightsIs400AndNothingIsPublished() throws Exception {
        Long id = steamCandidate(2, "Empty", "empty-2");
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\",\"highlights\":[],\"sponsored\":false}"))
                .andExpect(status().isOk());

        mvc.perform(admin(post("/api/admin/games/{id}/publish", id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(admin(get("/api/admin/games").param("q", "Empty")))
                .andExpect(jsonPath("$.content[0].status").value("CANDIDATE"))
                .andExpect(jsonPath("$.content[0].articleStatus").value("DRAFT"));
    }

    @Test
    void duplicateSlugIs409() throws Exception {
        steamCandidate(3, "Taken", "taken");
        Long id = steamCandidate(4, "Other", "other-4");

        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"taken\",\"genreSlugs\":[]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownGenreAndSteamCoopAre400() throws Exception {
        Long id = steamCandidate(5, "Steam Game", "steam-game-5");

        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"steam-game\",\"genreSlugs\":[\"psychological\",\"no-such\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown genre slugs: [no-such]"));
        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"steam-game\",\"genreSlugs\":[],\"coop\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"Not A Slug\",\"genreSlugs\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualGameCoopCanBeSet() throws Exception {
        Long id = gameRepository.save(Game.manual("Indie", "indie")).getId();

        mvc.perform(admin(put("/api/admin/games/{id}/curation", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"indie\",\"genreSlugs\":[\"analog\"],\"coop\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coop").value(true));
    }

    @Test
    void sponsoredArticleRequiresDisclosure() throws Exception {
        Long id = steamCandidate(6, "Ad", "ad-6");

        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\",\"highlights\":[\"a\"],"
                                + "\"sponsored\":true,\"sponsorDisclosure\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\",\"highlights\":[\"a\"],"
                                + "\"sponsored\":true,\"sponsorDisclosure\":\"Key provided\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sponsored").value(true))
                .andExpect(jsonPath("$.sponsorDisclosure").value("Key provided"));
    }

    @Test
    void unknownGameIs404AndMissingKeyIs403() throws Exception {
        mvc.perform(admin(post("/api/admin/games/{id}/publish", 9999))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/games")).andExpect(status().isForbidden());
    }

    private Long steamCandidate(int appid, String title, String slug) {
        Game game = Game.candidateFromSteam(appid, title, slug);
        game.applySteamData(new SteamGameData(title, "desc", "https://img/" + appid, LocalDate.of(2020, 9, 18),
                "2020년 9월 18일", false, false,
                List.of(new SteamGameData.Media(com.horrorpond.catalog.domain.MediaType.SCREENSHOT,
                        "https://s/" + appid, null)),
                List.of()));
        return gameRepository.save(game).getId();
    }

    private static MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.header("X-Admin-Key", KEY);
    }
}
