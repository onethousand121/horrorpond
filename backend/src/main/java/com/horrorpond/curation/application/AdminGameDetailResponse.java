package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.domain.StoreLink;
import com.horrorpond.catalog.domain.TrendingOverride;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.EndingGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.KoreanPatchResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.domain.CurationArticle;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 편집 화면용 단건 조회. 글이 없으면 article은 null.
 */
public record AdminGameDetailResponse(
        Long id,
        GameSource source,
        String externalId,
        String slug,
        String title,
        String shortDescription,
        String headerImageUrl,
        LocalDate releaseDate,
        boolean comingSoon,
        boolean coop,
        Integer reviewCount,
        boolean adult,
        List<String> tags,
        GameStatus status,
        /** 지금 공개 사이트에 보이는지 (자동 노출 포함) */
        boolean publiclyVisible,
        List<String> genreSlugs,
        String steamUrl,
        AdminArticleResponse article,
        List<PlayVideoResponse> playVideos,
        List<AchievementGuideResponse> achievements,
        /** itch.io 게임 주소 (ITCH 게임만) */
        String itchUrl,
        /** "지금 뜨는" 고정(PINNED)·제외(EXCLUDED). null이면 자동 */
        TrendingOverride trendingOverride,
        /** 다른 채널의 참고 영상 */
        List<PlayVideoResponse> referenceVideos,
        /** "지금 뜨는" 고정을 매주 조사 반영이 했으면 true */
        boolean trendingAuto,
        /** 엔딩 구분. null이면 표시 안 함 */
        EndingType endingType,
        /** 엔딩 공략 (멀티 엔딩일 때만) */
        List<EndingGuideResponse> endings,
        /** 유저 한글패치 */
        List<KoreanPatchResponse> koreanPatches
) {

    static AdminGameDetailResponse of(Game game, CurationArticle article, ExposurePolicy policy,
                                      List<PlayVideoResponse> playVideos,
                                      List<AchievementGuideResponse> achievements,
                                      List<PlayVideoResponse> referenceVideos,
                                      List<EndingGuideResponse> endings,
                                      List<KoreanPatchResponse> koreanPatches) {
        List<String> genreSlugs = game.getGenres().stream().map(Genre::getSlug).sorted().toList();
        String steamUrl = storeUrl(game, Store.STEAM);
        return new AdminGameDetailResponse(game.getId(), game.getSource(), game.getExternalId(), game.getSlug(),
                game.getTitle(), game.getShortDescription(), game.getHeaderImageUrl(), game.getReleaseDate(),
                game.isComingSoon(), game.isCoop(), game.getReviewCount(), game.isAdult(), List.copyOf(game.getTags()),
                game.getStatus(), policy.isVisible(game), genreSlugs, steamUrl,
                article == null ? null : AdminArticleResponse.from(article), playVideos, achievements,
                storeUrl(game, Store.ITCH), game.getTrendingOverride(), referenceVideos,
                game.isTrendingAuto(), game.getEndingType(), endings, koreanPatches);
    }

    private static String storeUrl(Game game, Store store) {
        return game.getStoreLinks().stream()
                .filter(link -> link.getStore() == store)
                .map(StoreLink::getUrl)
                .findFirst()
                .orElse(null);
    }
}
