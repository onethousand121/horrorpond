package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameDeveloper;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.EndingGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.KoreanPatchResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.application.GameSummaryResponse.GenreSummary;
import com.horrorpond.curation.domain.ArticleKind;
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
        /** 성인 콘텐츠. 소개·미디어·글·영상·공략·한글패치는 비워서 내려준다 (사이트는 Steam 링크만) */
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
        List<AchievementGuideResponse> achievements,
        /** STEAM | ITCH | MANUAL. ITCH면 reviewCount는 itch.io 평가 수 */
        GameSource source,
        /** 다른 채널의 참고 영상 (사이트는 주인장 영상이 없을 때만 보여준다). 없으면 빈 목록 */
        List<PlayVideoResponse> referenceVideos,
        /** 무료 게임 */
        boolean free,
        /** 정가(할인 전)의 최소 단위 금액 (KRW는 100배, USD는 센트). 모르면 null */
        Integer priceAmount,
        /** KRW, USD. priceAmount가 없으면 null */
        String priceCurrency,
        /** 엔딩 구분 SINGLE | MULTIPLE. 표시 안 함이면 null */
        EndingType endingType,
        /** 엔딩 공략 (멀티 엔딩일 때만). 없으면 빈 목록 */
        List<EndingGuideResponse> endings,
        /** 유저 한글패치 배포처. 없으면 빈 목록 */
        List<KoreanPatchResponse> koreanPatches
) {

    /**
     * 제목·소개·출시일 텍스트는 요청 언어로 (영어가 없으면 한국어). 큐레이터 글은 영어 번역이 있을 때만 영어로.
     * 성인 게임은 API에서부터 소개·미디어·글·영상·업적·엔딩 공략·한글패치를 비운다 (사이트는 Steam 링크만 보여준다).
     */
    static GameDetailResponse of(Game game, List<GenreSummary> genres, CurationArticle article, Language language,
                                 List<PlayVideoResponse> playVideos, List<AchievementGuideResponse> achievements,
                                 List<PlayVideoResponse> referenceVideos, List<EndingGuideResponse> endings,
                                 List<KoreanPatchResponse> koreanPatches) {
        boolean adult = game.isAdult();
        return new GameDetailResponse(
                game.getSlug(), game.title(language), adult ? null : game.shortDescription(language),
                !adult && game.isShortDescriptionAutoTranslated(language), game.getHeaderImageUrl(),
                game.getReleaseDate(), game.releaseDateText(language), game.isComingSoon(), game.isCoop(),
                adult,
                game.getReviewCount(), List.copyOf(game.getLanguages()), List.copyOf(game.getAudioLanguages()), genres,
                game.getDevelopers().stream()
                        .sorted(Comparator.comparing(GameDeveloper::getRole)
                                .thenComparing(credit -> credit.getDeveloper().getName()))
                        .map(DeveloperCredit::from)
                        .toList(),
                adult ? List.of() : game.getMedia().stream()
                        .map(media -> new Media(media.getType(), media.getUrl(), media.getThumbnailUrl()))
                        .toList(),
                game.getStoreLinks().stream()
                        .map(link -> new StoreLinkResponse(link.getStore(), link.getUrl()))
                        .toList(),
                adult || article == null ? null : Article.from(article, language), adult ? List.of() : playVideos,
                adult ? List.of() : achievements, game.getSource(),
                adult ? List.of() : referenceVideos, game.isFree(), game.getPriceAmount(), game.getPriceCurrency(),
                adult ? null : game.getEndingType(),
                adult || game.getEndingType() != EndingType.MULTIPLE ? List.of() : endings,
                adult ? List.of() : koreanPatches);
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
    /**
     * @param kind     PICK(추천) | REVIEW(플레이 후기)
     * @param language 글이 실제로 쓰인 언어 ko | en (영어 화면이라도 번역이 없으면 ko)
     */
    public record Article(String title, String oneLiner, String body, List<String> highlights,
                          boolean sponsored, String sponsorDisclosure, Instant publishedAt, ArticleKind kind,
                          String language) {

        static Article from(CurationArticle article, Language language) {
            return new Article(article.title(language), article.oneLiner(language), article.body(language),
                    List.copyOf(article.highlights(language)), article.isSponsored(), article.getSponsorDisclosure(),
                    article.getPublishedAt(), article.getKind(), article.isShownIn(language) ? "en" : "ko");
        }
    }
}
