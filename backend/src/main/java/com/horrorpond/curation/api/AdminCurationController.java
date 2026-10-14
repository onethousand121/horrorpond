package com.horrorpond.curation.api;

import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.common.web.PageResponse;
import com.horrorpond.curation.application.AdminArticleResponse;
import com.horrorpond.catalog.domain.TrendingOverride;
import com.horrorpond.curation.application.AdminGameDetailResponse;
import com.horrorpond.curation.domain.ArticleKind;
import com.horrorpond.curation.application.AdminGameDetailResponse;
import com.horrorpond.curation.application.AdminGameResponse;
import com.horrorpond.curation.application.CurationService;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.EndingsResponse;
import com.horrorpond.curation.application.GameGuideResponses.KoreanPatchResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.application.GameGuideService;
import com.horrorpond.curation.application.PublishingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * /api/admin/** 는 AdminKeyFilter가 X-Admin-Key를 검사한다.
 */
@RestController
@RequestMapping("/api/admin/games")
@RequiredArgsConstructor
public class AdminCurationController {

    private final CurationService curationService;
    private final PublishingService publishingService;
    private final GameGuideService gameGuideService;

    /**
     * 검토 대기열. status를 생략하면 전체, q는 제목 부분일치(대소문자 무시).
     */
    @GetMapping
    public PageResponse<AdminGameResponse> search(
            @RequestParam(required = false) GameStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(curationService.search(status, q, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public AdminGameDetailResponse detail(@PathVariable Long id) {
        return curationService.getDetail(id);
    }

    @PutMapping("/{id}/curation")
    public AdminGameResponse updateCuration(@PathVariable Long id, @Valid @RequestBody CurationRequest request) {
        return curationService.updateCuration(id, request.slug(), request.genreSlugs(), request.coop());
    }

    @PutMapping("/{id}/article")
    public AdminArticleResponse upsertArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        ArticleKind kind = request.kind() == null ? ArticleKind.PICK : request.kind();
        return curationService.upsertArticle(id, new CurationService.ArticleCommand(kind, request.title(),
                request.oneLiner(), request.body(), request.highlights(), request.sponsored(),
                request.sponsorDisclosure()));
    }

    /**
     * 플레이 영상 목록 전체를 바꾼다 (순서 = 목록 순서, 빈 목록이면 모두 삭제).
     */
    @PutMapping("/{id}/videos")
    public List<PlayVideoResponse> replacePlayVideos(@PathVariable Long id,
                                                     @Valid @RequestBody PlayVideosRequest request) {
        return gameGuideService.replacePlayVideos(id, request.videos().stream()
                .map(video -> new GameGuideService.PlayVideoCommand(video.url(), video.title()))
                .toList());
    }

    /**
     * 참고 영상(다른 채널) 목록 전체를 바꾼다. 주인장 영상(/videos)은 그대로 둔다.
     */
    @PutMapping("/{id}/reference-videos")
    public List<PlayVideoResponse> replaceReferenceVideos(@PathVariable Long id,
                                                          @Valid @RequestBody ReferenceVideosRequest request) {
        return gameGuideService.replaceReferenceVideos(id, request.videos().stream()
                .map(video -> new GameGuideService.ReferenceVideoCommand(video.url(), video.title(),
                        video.channelName()))
                .toList());
    }

    /**
     * 업적 공략 목록 전체를 바꾼다 (순서 = 목록 순서, 빈 목록이면 모두 삭제).
     */
    @PutMapping("/{id}/achievements")
    public List<AchievementGuideResponse> replaceAchievements(@PathVariable Long id,
                                                              @Valid @RequestBody AchievementsRequest request) {
        return gameGuideService.replaceAchievements(id, request.achievements().stream()
                .map(a -> new GameGuideService.AchievementCommand(a.name(), a.description(), a.videoUrl()))
                .toList());
    }

    /**
     * 엔딩 구분과 엔딩 목록을 한 번에 바꾼다. endingType이 null이면 표시 안 함.
     * 목록은 MULTIPLE일 때만 받는다 (그 외에 목록이 있으면 400, 빈 목록이면 기존 목록을 지운다).
     */
    @PutMapping("/{id}/endings")
    public EndingsResponse replaceEndings(@PathVariable Long id, @Valid @RequestBody EndingsRequest request) {
        return gameGuideService.replaceEndings(id, request.endingType(), request.endings().stream()
                .map(e -> new GameGuideService.EndingCommand(e.name(), e.description(), e.videoUrl()))
                .toList());
    }

    /**
     * 유저 한글패치 목록 전체를 바꾼다 (순서 = 목록 순서, 빈 목록이면 모두 삭제).
     */
    @PutMapping("/{id}/korean-patches")
    public List<KoreanPatchResponse> replaceKoreanPatches(@PathVariable Long id,
                                                          @Valid @RequestBody KoreanPatchesRequest request) {
        return gameGuideService.replaceKoreanPatches(id, request.patches().stream()
                .map(p -> new GameGuideService.KoreanPatchCommand(p.url(), p.author(), p.note()))
                .toList());
    }

    @PostMapping("/{id}/publish")
    public AdminGameResponse publish(@PathVariable Long id) {
        return publishingService.publish(id);
    }

    /** 추천 내리기 (글은 초안으로 남는다) */
    @PostMapping("/{id}/article/unpublish")
    public AdminGameResponse unpublishArticle(@PathVariable Long id) {
        return curationService.unpublishArticle(id);
    }

    /** 추천 글 삭제 */
    @DeleteMapping("/{id}/article")
    public AdminGameResponse deleteArticle(@PathVariable Long id) {
        return curationService.deleteArticle(id);
    }

    /**
     * "지금 뜨는" 고정(PINNED)·제외(EXCLUDED). override가 null이면 자동 기준으로 되돌린다.
     */
    @PutMapping("/{id}/trending")
    public AdminGameDetailResponse changeTrendingOverride(@PathVariable Long id,
                                                          @RequestBody TrendingOverrideRequest request) {
        return curationService.changeTrendingOverride(id, request.override(), Boolean.TRUE.equals(request.auto()));
    }

    /**
     * @param auto 매주 조사 반영이 고정하면 true (다음 반영 때 결과에서 빠지면 풀 수 있게 표시). 없으면 관리자 고정
     */
    public record TrendingOverrideRequest(TrendingOverride override, Boolean auto) {
    }

    /** "지금 뜨는"에 고정된 게임. auto=true는 매주 조사 반영이 고정한 것 */
    @GetMapping("/trending-pins")
    public List<CurationService.TrendingPin> trendingPins() {
        return curationService.trendingPins();
    }

    /** 상점 정보로 게임 찾기: ?steamAppId=123 또는 ?itchUrl=https://작성자.itch.io/게임. 없으면 404 */
    @GetMapping("/lookup")
    public AdminGameDetailResponse lookup(@RequestParam(required = false) Integer steamAppId,
                                          @RequestParam(required = false) String itchUrl) {
        return curationService.lookup(steamAppId, itchUrl);
    }

    @PostMapping("/{id}/hide")
    public AdminGameResponse hide(@PathVariable Long id) {
        return curationService.hide(id);
    }

    @PostMapping("/{id}/unhide")
    public AdminGameResponse unhide(@PathVariable Long id) {
        return curationService.unhide(id);
    }

    public record CurationRequest(
            @NotBlank @Size(max = 220) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                    message = "must be lowercase letters, digits and single hyphens") String slug,
            @NotNull List<@NotBlank String> genreSlugs,
            Boolean coop) {
    }

    /** 주소 형식·길이·개수는 도메인(PlayVideo, YoutubeVideoId)이 검증한다 */
    public record PlayVideosRequest(@NotNull List<@NotNull PlayVideoItem> videos) {
    }

    public record PlayVideoItem(String url, String title) {
    }

    /** 채널 이름 필수·주소·개수는 도메인(PlayVideo)이 검증한다 */
    public record ReferenceVideosRequest(@NotNull List<@NotNull ReferenceVideoItem> videos) {
    }

    public record ReferenceVideoItem(String url, String title, String channelName) {
    }

    /** 이름 필수·길이·개수는 도메인(AchievementGuide)이 검증한다 */
    public record AchievementsRequest(@NotNull List<@NotNull AchievementItem> achievements) {
    }

    public record AchievementItem(String name, String description, String videoUrl) {
    }

    /** 이름 필수·길이·개수·영상 주소는 도메인(EndingGuide)이, 구분과 목록의 짝은 GameGuideService가 검증한다 */
    public record EndingsRequest(EndingType endingType, @NotNull List<@NotNull EndingItem> endings) {
    }

    public record EndingItem(String name, String description, String videoUrl) {
    }

    /** https 주소·길이·개수는 도메인(KoreanPatch)이 검증한다 */
    public record KoreanPatchesRequest(@NotNull List<@NotNull KoreanPatchItem> patches) {
    }

    public record KoreanPatchItem(String url, String author, String note) {
    }

    /**
     * 길이·개수 등 글 규칙은 도메인(CurationArticle)이 검증한다. kind가 없으면 추천(PICK).
     */
    public record ArticleRequest(
            ArticleKind kind,
            String title,
            String oneLiner,
            String body,
            List<String> highlights,
            boolean sponsored,
            String sponsorDisclosure) {
    }
}
