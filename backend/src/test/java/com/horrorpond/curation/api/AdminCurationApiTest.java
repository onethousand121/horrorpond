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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .andExpect(jsonPath("$.content[0].headerImageUrl").value("https://img/739630"))
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
    void publishedPickCanBeUnpublishedAndDeleted() throws Exception {
        Long id = steamCandidate(3, "Pick", "pick-3");
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\",\"highlights\":[\"h\"],\"sponsored\":false}"))
                .andExpect(status().isOk());
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());
        mvc.perform(get("/api/games/pick-3")).andExpect(jsonPath("$.article.oneLiner").value("o"));

        // 추천 내리기: 글은 초안으로 남고 사이트에서는 사라진다. 게임은 고정 노출 그대로
        mvc.perform(admin(post("/api/admin/games/{id}/article/unpublish", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.hasArticle").value(true));
        mvc.perform(get("/api/games/pick-3")).andExpect(jsonPath("$.article").doesNotExist());
        mvc.perform(get("/api/games").param("picked", "true")).andExpect(jsonPath("$.content", hasSize(0)));

        // 다시 공개할 수 있고, 삭제하면 글이 없어진다
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());
        mvc.perform(get("/api/games/pick-3")).andExpect(jsonPath("$.article.oneLiner").value("o"));
        mvc.perform(admin(delete("/api/admin/games/{id}/article", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasArticle").value(false));
        mvc.perform(get("/api/games/pick-3")).andExpect(jsonPath("$.article").doesNotExist());
        mvc.perform(admin(post("/api/admin/games/{id}/article/unpublish", id))).andExpect(status().isNotFound());
    }

    @Test
    void reviewIsPublishedWithoutHighlightsAndIsNotAPick() throws Exception {
        Long id = steamCandidate(4, "Played", "played-4");
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"REVIEW\",\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\","
                                + "\"highlights\":[],\"sponsored\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("REVIEW"));
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());

        // 후기도 글은 그대로 보이지만, 추천 뱃지·추천 목록·주인장 추천에는 나오지 않는다
        mvc.perform(get("/api/games/played-4"))
                .andExpect(jsonPath("$.article.kind").value("REVIEW"))
                .andExpect(jsonPath("$.article.oneLiner").value("o"));
        mvc.perform(get("/api/games").param("q", "Played"))
                .andExpect(jsonPath("$.content[0].picked").value(false))
                .andExpect(jsonPath("$.content[0].reviewed").value(true))
                .andExpect(jsonPath("$.content[0].oneLiner").value("o"));
        mvc.perform(get("/api/games").param("picked", "true")).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/games").param("keeper", "true")).andExpect(jsonPath("$.content", hasSize(0)));

        // 추천으로 바꾸려면 장점 포인트가 있어야 한다 (공개된 글)
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"PICK\",\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\","
                                + "\"highlights\":[],\"sponsored\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"PICK\",\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\","
                                + "\"highlights\":[\"h\"],\"sponsored\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/games").param("picked", "true"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reviewed").value(false));
    }

    @Test
    void trendingCanBePinnedExcludedAndReset() throws Exception {
        Long id = steamCandidate(5, "Trend", "trend-5");
        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.trendingOverride").doesNotExist());

        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":\"PINNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendingOverride").value("PINNED"));
        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":\"EXCLUDED\"}"))
                .andExpect(jsonPath("$.trendingOverride").value("EXCLUDED"));
        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":null}"))
                .andExpect(jsonPath("$.trendingOverride").doesNotExist());
        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":\"TOP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lookupByStoreAndAutoPinsForWeeklyResearch() throws Exception {
        Long id = steamCandidate(77, "Lookup", "lookup-77");
        mvc.perform(admin(get("/api/admin/games/lookup").param("steamAppId", "77")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.steamUrl").value("https://store.steampowered.com/app/77"));
        mvc.perform(admin(get("/api/admin/games/lookup").param("steamAppId", "78"))).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/admin/games/lookup").param("itchUrl", "https://nobody.itch.io/none")))
                .andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/admin/games/lookup"))).andExpect(status().isBadRequest());

        // 조사 반영이 고정하면 auto=true, 관리자가 다시 고정하면 auto=false
        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":\"PINNED\",\"auto\":true}"))
                .andExpect(jsonPath("$.trendingAuto").value(true));
        mvc.perform(admin(get("/api/admin/games/trending-pins")))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].auto").value(true));
        mvc.perform(admin(put("/api/admin/games/{id}/trending", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"override\":\"PINNED\"}"))
                .andExpect(jsonPath("$.trendingAuto").value(false));
        mvc.perform(admin(get("/api/admin/games/trending-pins"))).andExpect(jsonPath("$[0].auto").value(false));
    }

    @Test
    void publishWithoutArticlePinsGameOnly() throws Exception {
        Long id = steamCandidate(1, "No Article", "no-article-1");
        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.publiclyVisible").value(false));

        mvc.perform(admin(post("/api/admin/games/{id}/publish", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publiclyVisible").value(true))
                .andExpect(jsonPath("$.hasArticle").value(false));
        mvc.perform(get("/api/games/no-article-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article").doesNotExist());
    }

    @Test
    void publishWithDraftWithoutHighlightsPinsGameAndKeepsDraft() throws Exception {
        Long id = steamCandidate(2, "Empty", "empty-2");
        mvc.perform(admin(put("/api/admin/games/{id}/article", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"oneLiner\":\"o\",\"body\":\"b\",\"highlights\":[],\"sponsored\":false}"))
                .andExpect(status().isOk());

        mvc.perform(admin(post("/api/admin/games/{id}/publish", id)))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/admin/games").param("q", "Empty")))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
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
    void referenceVideosAreSeparateFromKeeperVideos() throws Exception {
        Long id = steamCandidate(6, "Referenced", "referenced-6");
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());

        mvc.perform(admin(put("/api/admin/games/{id}/reference-videos", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"videos":[{"url":"https://youtu.be/dQw4w9WgXcQ","title":"Full playthrough","channelName":" Some Channel "}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelName").value("Some Channel"));

        // 참고 영상은 따로 내려가고, 주인장 영상 뱃지·주인장 추천 줄에는 세지 않는다
        mvc.perform(get("/api/games/referenced-6"))
                .andExpect(jsonPath("$.playVideos", hasSize(0)))
                .andExpect(jsonPath("$.referenceVideos[0].youtubeId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.referenceVideos[0].channelName").value("Some Channel"));
        mvc.perform(get("/api/games").param("q", "Referenced"))
                .andExpect(jsonPath("$.content[0].hasPlayVideo").value(false));
        mvc.perform(get("/api/games").param("keeper", "true")).andExpect(jsonPath("$.content", hasSize(0)));

        // 주인장 영상을 저장해도 참고 영상은 남는다 (반대도 마찬가지)
        mvc.perform(admin(put("/api/admin/games/{id}/videos", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videos\":[{\"url\":\"https://youtu.be/aBcDeFgHiJk\",\"title\":\"\"}]}"))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.playVideos", hasSize(1)))
                .andExpect(jsonPath("$.playVideos[0].channelName").doesNotExist())
                .andExpect(jsonPath("$.referenceVideos", hasSize(1)));
        mvc.perform(get("/api/games").param("q", "Referenced"))
                .andExpect(jsonPath("$.content[0].hasPlayVideo").value(true));

        // 채널 이름이 없으면 400
        mvc.perform(admin(put("/api/admin/games/{id}/reference-videos", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videos\":[{\"url\":\"https://youtu.be/dQw4w9WgXcQ\",\"title\":\"x\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void playVideosAndAchievementsAreReplacedAsWholeLists() throws Exception {
        Long id = steamCandidate(1, "Guided", "guided-1");
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());
        // 기본은 비어 있다
        mvc.perform(get("/api/games/guided-1"))
                .andExpect(jsonPath("$.playVideos", hasSize(0)))
                .andExpect(jsonPath("$.achievements", hasSize(0)));

        mvc.perform(admin(put("/api/admin/games/{id}/videos", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"videos":[{"url":"https://youtu.be/dQw4w9WgXcQ","title":" 1화 "},
                                           {"url":"https://www.youtube.com/watch?v=aBcDeFgHiJk","title":""}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].youtubeId", contains("dQw4w9WgXcQ", "aBcDeFgHiJk")));
        mvc.perform(admin(put("/api/admin/games/{id}/achievements", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievements":[{"name":"Survivor","description":"Finish without dying","videoUrl":"https://youtu.be/dQw4w9WgXcQ"},
                                                 {"name":"Explorer","description":"","videoUrl":""}]}"""))
                .andExpect(status().isOk());

        mvc.perform(get("/api/games/guided-1"))
                .andExpect(jsonPath("$.playVideos[0].title").value("1화"))
                .andExpect(jsonPath("$.playVideos[1].title").doesNotExist())
                .andExpect(jsonPath("$.achievements[*].name", contains("Survivor", "Explorer")))
                .andExpect(jsonPath("$.achievements[0].youtubeId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.achievements[1].youtubeId").doesNotExist())
                .andExpect(jsonPath("$.achievements[1].description").doesNotExist());
        mvc.perform(get("/api/games"))
                .andExpect(jsonPath("$.content[0].hasPlayVideo").value(true))
                .andExpect(jsonPath("$.content[0].hasAchievementGuide").value(true));
        // 주인장 추천 목록: 주인장 플레이 영상이 있는 게임
        mvc.perform(get("/api/games").param("keeper", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("guided-1")));
        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.playVideos", hasSize(2)))
                .andExpect(jsonPath("$.achievements", hasSize(2)));

        // 잘못된 주소가 하나라도 있으면 400이고 기존 목록은 그대로
        mvc.perform(admin(put("/api/admin/games/{id}/videos", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"videos\":[{\"url\":\"https://vimeo.com/1\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/achievements", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievements\":[{\"name\":\" \"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/guided-1"))
                .andExpect(jsonPath("$.playVideos", hasSize(2)))
                .andExpect(jsonPath("$.achievements", hasSize(2)));

        // 빈 목록이면 모두 지워져 사이트에서 사라진다
        mvc.perform(admin(put("/api/admin/games/{id}/videos", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"videos\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/games/guided-1")).andExpect(jsonPath("$.playVideos", hasSize(0)));
        mvc.perform(get("/api/games")).andExpect(jsonPath("$.content[0].hasPlayVideo").value(false));
        mvc.perform(get("/api/games").param("keeper", "true")).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void endingTypeAndEndingsAreReplacedTogether() throws Exception {
        Long id = steamCandidate(1, "Endings", "endings-1");
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());
        // 기본은 표시 안 함
        mvc.perform(get("/api/games/endings-1"))
                .andExpect(jsonPath("$.endingType").doesNotExist())
                .andExpect(jsonPath("$.endings", hasSize(0)));
        mvc.perform(get("/api/games")).andExpect(jsonPath("$.content[0].multiEnding").value(false));

        // 멀티 엔딩: 구분과 목록을 한 번에 (쇼츠 주소도 받는다)
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endingType":"MULTIPLE","endings":[
                                  {"name":" 탈출 ","description":"열쇠를 모두 모은다\\n그리고 문으로","videoUrl":"https://www.youtube.com/shorts/dQw4w9WgXcQ"},
                                  {"name":"죽음","description":"","videoUrl":""}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endingType").value("MULTIPLE"))
                .andExpect(jsonPath("$.endings[*].name", contains("탈출", "죽음")));
        mvc.perform(get("/api/games/endings-1"))
                .andExpect(jsonPath("$.endingType").value("MULTIPLE"))
                .andExpect(jsonPath("$.endings[0].youtubeId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.endings[0].description").value("열쇠를 모두 모은다\n그리고 문으로"))
                .andExpect(jsonPath("$.endings[1].youtubeId").doesNotExist())
                .andExpect(jsonPath("$.endings[1].description").doesNotExist());
        mvc.perform(get("/api/games")).andExpect(jsonPath("$.content[0].multiEnding").value(true));
        mvc.perform(admin(get("/api/admin/games/{id}", id)))
                .andExpect(jsonPath("$.endingType").value("MULTIPLE"))
                .andExpect(jsonPath("$.endings", hasSize(2)));

        // 멀티 엔딩이 아닌데 목록이 있거나, 이름·주소가 잘못되면 400이고 기존 값은 그대로
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endingType\":\"SINGLE\",\"endings\":[{\"name\":\"하나\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endingType\":null,\"endings\":[{\"name\":\"하나\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endingType\":\"MULTIPLE\",\"endings\":[{\"name\":\"x\",\"videoUrl\":\"https://vimeo.com/1\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/endings-1"))
                .andExpect(jsonPath("$.endingType").value("MULTIPLE"))
                .andExpect(jsonPath("$.endings", hasSize(2)));

        // 단일 엔딩으로 바꾸면 목록은 지워진다
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endingType\":\"SINGLE\",\"endings\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endingType").value("SINGLE"));
        mvc.perform(get("/api/games/endings-1"))
                .andExpect(jsonPath("$.endingType").value("SINGLE"))
                .andExpect(jsonPath("$.endings", hasSize(0)));
        mvc.perform(get("/api/games")).andExpect(jsonPath("$.content[0].multiEnding").value(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ending_guide", Integer.class)).isZero();

        // 표시 안 함으로 되돌린다
        mvc.perform(admin(put("/api/admin/games/{id}/endings", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endingType\":null,\"endings\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/games/endings-1")).andExpect(jsonPath("$.endingType").doesNotExist());
        mvc.perform(admin(put("/api/admin/games/{id}/endings", 999999))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endings\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void koreanPatchesAreReplacedAsWholeListAndCountForKoreanFilter() throws Exception {
        Long id = steamCandidate(1, "Patched", "patched-1");
        Long other = steamCandidate(2, "Plain", "plain-2");
        mvc.perform(admin(post("/api/admin/games/{id}/publish", id))).andExpect(status().isOk());
        mvc.perform(admin(post("/api/admin/games/{id}/publish", other))).andExpect(status().isOk());
        mvc.perform(get("/api/games").param("korean", "true")).andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(admin(put("/api/admin/games/{id}/korean-patches", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patches":[{"url":" https://cafe.example.com/patch/1 ","author":"한글팀","note":"v1.2 기준"},
                                            {"url":"https://blog.example.kr/p","author":"","note":""}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].url", contains("https://cafe.example.com/patch/1", "https://blog.example.kr/p")));
        mvc.perform(get("/api/games/patched-1"))
                .andExpect(jsonPath("$.koreanPatches[0].author").value("한글팀"))
                .andExpect(jsonPath("$.koreanPatches[0].note").value("v1.2 기준"))
                .andExpect(jsonPath("$.koreanPatches[1].author").doesNotExist());
        mvc.perform(get("/api/games/plain-2")).andExpect(jsonPath("$.koreanPatches", hasSize(0)));
        // 한국어 필터는 한글패치가 있는 게임도 포함한다
        mvc.perform(get("/api/games").param("korean", "true"))
                .andExpect(jsonPath("$.content[*].slug", contains("patched-1")))
                .andExpect(jsonPath("$.content[0].koreanPatch").value(true));
        mvc.perform(admin(get("/api/admin/games/{id}", id))).andExpect(jsonPath("$.koreanPatches", hasSize(2)));

        // https가 아니면 400이고 기존 목록은 그대로
        for (String url : List.of("http://example.com/p", "javascript:alert(1)", "/relative", "https:///nohost", "")) {
            mvc.perform(admin(put("/api/admin/games/{id}/korean-patches", id))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patches\":[{\"url\":\"" + url + "\"}]}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/games/patched-1")).andExpect(jsonPath("$.koreanPatches", hasSize(2)));

        // 빈 목록이면 모두 지워진다
        mvc.perform(admin(put("/api/admin/games/{id}/korean-patches", id))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"patches\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/games").param("korean", "true")).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void tooManyPlayVideosIs400AndUnknownGameIs404() throws Exception {
        Long id = steamCandidate(1, "Many", "many-1");
        String eleven = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> "{\"url\":\"dQw4w9WgXcQ\"}")
                .collect(java.util.stream.Collectors.joining(",", "{\"videos\":[", "]}"));
        mvc.perform(admin(put("/api/admin/games/{id}/videos", id))
                        .contentType(MediaType.APPLICATION_JSON).content(eleven))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/admin/games/{id}/videos", 999999))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"videos\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownGameIs404AndMissingKeyIs403() throws Exception {
        mvc.perform(admin(post("/api/admin/games/{id}/publish", 9999))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/games")).andExpect(status().isForbidden());
    }

    private Long steamCandidate(int appid, String title, String slug) {
        Game game = Game.candidateFromSteam(appid, title, slug);
        game.applySteamData(new SteamGameData(title, "desc", "https://img/" + appid, LocalDate.of(2020, 9, 18),
                "2020년 9월 18일", false, false, null, false,
                List.of(new SteamGameData.Media(com.horrorpond.catalog.domain.MediaType.SCREENSHOT,
                        "https://s/" + appid, null)),
                List.of()));
        return gameRepository.save(game).getId();
    }

    private static MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.header("X-Admin-Key", KEY);
    }
}
