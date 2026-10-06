package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.domain.StoreLink;
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
        boolean coop,
        GameStatus status,
        List<String> genreSlugs,
        String steamUrl,
        AdminArticleResponse article
) {

    static AdminGameDetailResponse of(Game game, CurationArticle article) {
        List<String> genreSlugs = game.getGenres().stream().map(Genre::getSlug).sorted().toList();
        String steamUrl = game.getStoreLinks().stream()
                .filter(link -> link.getStore() == Store.STEAM)
                .map(StoreLink::getUrl)
                .findFirst()
                .orElse(null);
        return new AdminGameDetailResponse(game.getId(), game.getSource(), game.getExternalId(), game.getSlug(),
                game.getTitle(), game.getShortDescription(), game.getHeaderImageUrl(), game.getReleaseDate(),
                game.isCoop(), game.getStatus(), genreSlugs, steamUrl,
                article == null ? null : AdminArticleResponse.from(article));
    }
}
