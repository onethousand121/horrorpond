package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.CuratedGameRow;

import java.time.LocalDate;
import java.util.List;

/**
 * @param picked     재일 추천(공개된 큐레이션 글이 있는 게임). 아니면 oneLiner는 null, highlights는 빈 목록
 * @param reviewCount Steam 리뷰 수. 없으면 null
 * @param tags        SteamSpy 상위 태그 (표 많은 순, 최대 5개)
 * @param hasPlayVideo 플레이 영상이 하나 이상 있으면 true
 * @param hasAchievementGuide 업적 공략이 하나 이상 있으면 true (업적 공략 페이지가 있다)
 * @param shortDescriptionTranslated 소개가 자동 번역이면 true (사이트에 "자동 번역" 표시)
 * @param adult       성인 콘텐츠 (고정 노출한 경우에만 목록에 나온다. 사이트는 이미지를 흐리게 보여준다)
 */
public record GameSummaryResponse(
        String slug,
        String title,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        String shortDescription,
        boolean shortDescriptionTranslated,
        boolean comingSoon,
        boolean coop,
        boolean adult,
        Integer reviewCount,
        List<String> tags,
        List<GenreSummary> genres,
        boolean picked,
        String oneLiner,
        List<String> highlights,
        boolean sponsored,
        boolean hasPlayVideo,
        boolean hasAchievementGuide
) {

    static final int MAX_HIGHLIGHTS = 3;
    static final int MAX_TAGS = 5;

    /**
     * 제목·소개·출시일 텍스트는 요청 언어로 (영어가 없으면 한국어). 큐레이터 글은 한국어만 있다.
     */
    static GameSummaryResponse of(CuratedGameRow row, List<GenreSummary> genres, Language language) {
        List<String> highlights = row.highlights() == null ? List.of() : row.highlights();
        List<String> tags = row.tags() == null ? List.of() : row.tags();
        return new GameSummaryResponse(row.slug(), language.pick(row.title(), row.titleEn()), row.headerImageUrl(),
                row.releaseDate(), language.pick(row.releaseDateText(), row.releaseDateTextEn()),
                Game.shortDescription(language, row.shortDescription(), row.shortDescriptionEn(),
                        row.shortDescriptionKoAuto()),
                Game.isShortDescriptionAutoTranslated(language, row.shortDescriptionKoAuto()), row.comingSoon(), row.coop(), row.adult(), row.reviewCount(),
                List.copyOf(tags.subList(0, Math.min(MAX_TAGS, tags.size()))), genres, row.picked(),
                row.oneLiner(),
                List.copyOf(highlights.subList(0, Math.min(MAX_HIGHLIGHTS, highlights.size()))),
                row.sponsored(), row.hasPlayVideo(), row.hasAchievementGuide());
    }

    public record GenreSummary(String slug, String name) {
    }
}
