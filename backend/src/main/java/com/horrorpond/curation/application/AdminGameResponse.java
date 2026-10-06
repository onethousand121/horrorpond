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
        String headerImageUrl,
        LocalDate releaseDate,
        boolean coop,
        GameStatus status,
        boolean hasArticle,
        ArticleStatus articleStatus,
        /** 목록 조회에서만 채운다. 단건 변경 응답에서는 0 */
        long sameTitleCount
) {

    static AdminGameResponse from(AdminGameRow row) {
        return new AdminGameResponse(row.id(), row.source(), row.externalId(), row.slug(), row.title(), row.headerImageUrl(),
                row.releaseDate(), row.coop(), row.status(), row.articleStatus() != null, row.articleStatus(),
                row.sameTitleCount());
    }

    static AdminGameResponse of(Game game, CurationArticle article) {
        ArticleStatus articleStatus = article == null ? null : article.getStatus();
        return new AdminGameResponse(game.getId(), game.getSource(), game.getExternalId(), game.getSlug(),
                game.getTitle(), game.getHeaderImageUrl(), game.getReleaseDate(), game.isCoop(), game.getStatus(),
                article != null, articleStatus, 0);
    }
}
