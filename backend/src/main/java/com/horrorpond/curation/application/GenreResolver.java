package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.application.GameSummaryResponse.GenreSummary;

import java.util.Collection;
import java.util.List;

/**
 * 게임의 장르 = 큐레이터가 붙인 장르 + SteamSpy 태그가 장르 태그와 겹치는 장르. 장르 표시 순서(displayOrder)를 따른다.
 */
final class GenreResolver {

    private final List<Genre> genres;

    /**
     * @param genresInOrder 전체 장르, displayOrder 순
     */
    GenreResolver(List<Genre> genresInOrder) {
        this.genres = genresInOrder;
    }

    List<GenreSummary> resolve(Collection<String> curatorSlugs, List<String> tags, Language language) {
        return genres.stream()
                .filter(genre -> curatorSlugs.contains(genre.getSlug()) || genre.matchesAnyTag(tags))
                .map(genre -> new GenreSummary(genre.getSlug(), genre.name(language)))
                .toList();
    }
}
