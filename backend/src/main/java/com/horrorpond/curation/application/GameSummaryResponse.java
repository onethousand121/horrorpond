package com.horrorpond.curation.application;

import com.horrorpond.curation.repository.CuratedGameQueryRepository.CuratedGameRow;

import java.time.LocalDate;
import java.util.List;

/**
 * @param picked     재일 추천(공개된 큐레이션 글이 있는 게임). 아니면 oneLiner는 null, highlights는 빈 목록
 * @param reviewCount Steam 리뷰 수. 없으면 null
 * @param tags        SteamSpy 상위 태그 (표 많은 순, 최대 5개)
 */
public record GameSummaryResponse(
        String slug,
        String title,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        String shortDescription,
        boolean comingSoon,
        boolean coop,
        Integer reviewCount,
        List<String> tags,
        List<GenreSummary> genres,
        boolean picked,
        String oneLiner,
        List<String> highlights,
        boolean sponsored
) {

    static final int MAX_HIGHLIGHTS = 3;
    static final int MAX_TAGS = 5;

    static GameSummaryResponse of(CuratedGameRow row, List<GenreSummary> genres) {
        List<String> highlights = row.highlights() == null ? List.of() : row.highlights();
        List<String> tags = row.tags() == null ? List.of() : row.tags();
        return new GameSummaryResponse(row.slug(), row.title(), row.headerImageUrl(), row.releaseDate(),
                row.releaseDateText(), row.shortDescription(), row.comingSoon(), row.coop(), row.reviewCount(),
                List.copyOf(tags.subList(0, Math.min(MAX_TAGS, tags.size()))), genres, row.picked(),
                row.oneLiner(),
                List.copyOf(highlights.subList(0, Math.min(MAX_HIGHLIGHTS, highlights.size()))),
                row.sponsored());
    }

    public record GenreSummary(String slug, String name) {
    }
}
