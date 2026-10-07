package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.AdminGameQueryRepository.AdminGameRow;

import java.time.LocalDate;

/**
 * @param publiclyVisible 지금 공개 사이트에 보이는지 (자동 노출 포함, ExposurePolicy)
 * @param sameTitleCount  목록 조회에서만 채운다. 단건 변경 응답에서는 0
 */
public record AdminGameResponse(
        Long id,
        GameSource source,
        String externalId,
        String slug,
        String title,
        String headerImageUrl,
        LocalDate releaseDate,
        boolean comingSoon,
        boolean coop,
        Integer reviewCount,
        boolean adult,
        GameStatus status,
        boolean publiclyVisible,
        boolean hasArticle,
        ArticleStatus articleStatus,
        long sameTitleCount
) {

    static AdminGameResponse from(AdminGameRow row, ExposurePolicy policy) {
        return new AdminGameResponse(row.id(), row.source(), row.externalId(), row.slug(), row.title(),
                row.headerImageUrl(), row.releaseDate(), row.comingSoon(), row.coop(), row.reviewCount(), row.adult(),
                row.status(), policy.isVisible(row.status(), row.adult(), row.comingSoon(), row.reviewCount(),
                        row.releaseDate()),
                row.articleStatus() != null, row.articleStatus(), row.sameTitleCount());
    }

    static AdminGameResponse of(Game game, CurationArticle article, ExposurePolicy policy) {
        ArticleStatus articleStatus = article == null ? null : article.getStatus();
        return new AdminGameResponse(game.getId(), game.getSource(), game.getExternalId(), game.getSlug(),
                game.getTitle(), game.getHeaderImageUrl(), game.getReleaseDate(), game.isComingSoon(), game.isCoop(),
                game.getReviewCount(), game.isAdult(), game.getStatus(), policy.isVisible(game),
                article != null, articleStatus, 0);
    }
}
