package com.horrorpond.curation.application;

import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;

import java.time.Instant;
import java.util.List;

public record AdminArticleResponse(
        Long id,
        Long gameId,
        ArticleStatus status,
        String title,
        String oneLiner,
        String body,
        List<String> highlights,
        boolean sponsored,
        String sponsorDisclosure,
        Instant publishedAt
) {

    static AdminArticleResponse from(CurationArticle article) {
        return new AdminArticleResponse(article.getId(), article.getGameId(), article.getStatus(),
                article.getTitle(), article.getOneLiner(), article.getBody(), List.copyOf(article.getHighlights()),
                article.isSponsored(), article.getSponsorDisclosure(), article.getPublishedAt());
    }
}
