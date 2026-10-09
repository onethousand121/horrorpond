package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameDeveloper;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.application.GameSummaryResponse.GenreSummary;
import com.horrorpond.curation.domain.CurationArticle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record GameDetailResponse(
        String slug,
        String title,
        String shortDescription,
        /** 소개가 자동 번역이면 true (사이트에 "자동 번역" 표시) */
        boolean shortDescriptionTranslated,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        boolean comingSoon,
        boolean coop,
        /** 성인 콘텐츠. 사이트는 소개·미디어 없이 Steam 링크만 보여준다 */
        boolean adult,
        Integer reviewCount,
        /** Steam 지원 언어 코드 (ko, en, ja …) */
        List<String> languages,
        /** 음성까지 지원하는 언어 코드 */
        List<String> audioLanguages,
        List<GenreSummary> genres,
        List<DeveloperCredit> developers,
        List<Media> media,
        List<StoreLinkResponse> storeLinks,
        /** 재일 추천 글. 없으면 null */
        Article article,
        /** 플레이 영상. 없으면 빈 목록 */
        List<PlayVideoResponse> playVideos,
        /** 업적 공략. 없으면 빈 목록 */
        List<AchievementGuideResponse> achievements
) {

    /**
     * 제목·소개·출시일 텍스트는 요청 언어로 (영어가 없으면 한국어). 큐레이터 글은 한국어만 있다.
     */
    static GameDetailResponse of(Game game, List<GenreSummary> genres, CurationArticle article, Language language,
                                 List<PlayVideoResponse> playVideos, List<AchievementGuideResponse> achievements) {
        return new GameDetailResponse(
                game.getSlug(), game.title(language), game.shortDescription(language),
                game.isShortDescriptionAutoTranslated(language), game.getHeaderImageUrl(),
                game.getReleaseDate(), game.releaseDateText(language), game.isComingSoon(), game.isCoop(),
                game.isAdult(),
                game.getReviewCount(), List.copyOf(game.getLanguages()), List.copyOf(game.getAudioLanguages()), genres,
                game.getDevelopers().stream()
                        .sorted(Comparator.comparing(GameDeveloper::getRole)
                                .thenComparing(credit -> credit.getDeveloper().getName()))
                        .map(DeveloperCredit::from)
                        .toList(),
                game.getMedia().stream()
                        .map(media -> new Media(media.getType(), media.getUrl(), media.getThumbnailUrl()))
                        .toList(),
                game.getStoreLinks().stream()
                        .map(link -> new StoreLinkResponse(link.getStore(), link.getUrl()))
                        .toList(),
                article == null ? null : Article.from(article), playVideos, achievements);
    }

    public record DeveloperCredit(String name, String slug, DeveloperRole role) {

        static DeveloperCredit from(GameDeveloper credit) {
            return new DeveloperCredit(credit.getDeveloper().getName(), credit.getDeveloper().getSlug(),
                    credit.getRole());
        }
    }

    public record Media(MediaType type, String url, String thumbnailUrl) {
    }

    public record StoreLinkResponse(Store store, String url) {
    }

    /**
     * @param body markdown 원문. 렌더링은 프론트엔드가 한다.
     */
    public record Article(String title, String oneLiner, String body, List<String> highlights,
                          boolean sponsored, String sponsorDisclosure, Instant publishedAt) {

        static Article from(CurationArticle article) {
            return new Article(article.getTitle(), article.getOneLiner(), article.getBody(),
                    List.copyOf(article.getHighlights()), article.isSponsored(), article.getSponsorDisclosure(),
                    article.getPublishedAt());
        }
    }
}
