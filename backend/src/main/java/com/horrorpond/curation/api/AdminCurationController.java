package com.horrorpond.curation.api;

import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.common.web.PageResponse;
import com.horrorpond.curation.application.AdminArticleResponse;
import com.horrorpond.curation.application.AdminGameDetailResponse;
import com.horrorpond.curation.application.AdminGameResponse;
import com.horrorpond.curation.application.CurationService;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
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
        return curationService.upsertArticle(id, new CurationService.ArticleCommand(request.title(),
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
     * 업적 공략 목록 전체를 바꾼다 (순서 = 목록 순서, 빈 목록이면 모두 삭제).
     */
    @PutMapping("/{id}/achievements")
    public List<AchievementGuideResponse> replaceAchievements(@PathVariable Long id,
                                                              @Valid @RequestBody AchievementsRequest request) {
        return gameGuideService.replaceAchievements(id, request.achievements().stream()
                .map(a -> new GameGuideService.AchievementCommand(a.name(), a.description(), a.videoUrl()))
                .toList());
    }

    @PostMapping("/{id}/publish")
    public AdminGameResponse publish(@PathVariable Long id) {
        return publishingService.publish(id);
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

    /** 이름 필수·길이·개수는 도메인(AchievementGuide)이 검증한다 */
    public record AchievementsRequest(@NotNull List<@NotNull AchievementItem> achievements) {
    }

    public record AchievementItem(String name, String description, String videoUrl) {
    }

    /**
     * 길이·개수 등 글 규칙은 도메인(CurationArticle)이 검증한다.
     */
    public record ArticleRequest(
            String title,
            String oneLiner,
            String body,
            List<String> highlights,
            boolean sponsored,
            String sponsorDisclosure) {
    }
}
