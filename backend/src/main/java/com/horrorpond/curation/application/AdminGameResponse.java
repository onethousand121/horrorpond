package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.AdminGameQueryRepository.AdminGameRow;

import java.time.LocalDate;

public record AdminGameResponse(
        Long id,
        GameSource source,
        String externalId,
        String slug,
        String title,
        LocalDate releaseDate,
        boolean coop,
        GameStatus status,
        boolean hasArticle,
        ArticleStatus articleStatus
) {

    static AdminGameResponse from(AdminGameRow row) {
        return new AdminGameResponse(row.id(), row.source(), row.externalId(), row.slug(), row.title(),
                row.releaseDate(), row.coop(), row.status(), row.articleStatus() != null, row.articleStatus());
    }

    static AdminGameResponse of(Game game, CurationArticle article) {
        ArticleStatus articleStatus = article == null ? null : article.getStatus();
        return new AdminGameResponse(game.getId(), game.getSource(), game.getExternalId(), game.getSlug(),
                game.getTitle(), game.getReleaseDate(), game.isCoop(), game.getStatus(),
                article != null, articleStatus);
    }
}
