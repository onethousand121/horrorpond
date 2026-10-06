package com.horrorpond.curation.application;

import com.horrorpond.curation.repository.CuratedGameQueryRepository.CuratedGameRow;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.GenreRow;

import java.time.LocalDate;
import java.util.List;

public record GameSummaryResponse(
        String slug,
        String title,
        String headerImageUrl,
        LocalDate releaseDate,
        boolean comingSoon,
        boolean coop,
        List<GenreSummary> genres,
        String oneLiner,
        List<String> highlights,
        boolean sponsored
) {

    static final int MAX_HIGHLIGHTS = 3;

    static GameSummaryResponse of(CuratedGameRow row, List<GenreRow> genres) {
        List<String> highlights = row.highlights() == null ? List.of() : row.highlights();
        return new GameSummaryResponse(row.slug(), row.title(), row.headerImageUrl(), row.releaseDate(),
                row.comingSoon(), row.coop(),
                genres.stream().map(g -> new GenreSummary(g.slug(), g.name())).toList(),
                row.oneLiner(),
                List.copyOf(highlights.subList(0, Math.min(MAX_HIGHLIGHTS, highlights.size()))),
                row.sponsored());
    }

    public record GenreSummary(String slug, String name) {
    }
}
