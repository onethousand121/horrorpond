package com.horrorpond.curation.application;

import com.horrorpond.curation.domain.ArticleKind;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.CurationArticle;

import java.time.Instant;
import java.util.List;

public record AdminArticleResponse(
        Long id,
        Long gameId,
        ArticleStatus status,
        ArticleKind kind,
        String title,
        String oneLiner,
        String body,
        List<String> highlights,
        boolean sponsored,
        String sponsorDisclosure,
        Instant publishedAt,
        /** 영어 번역. 없으면 titleEn·oneLinerEn·bodyEn이 null, highlightsEn은 빈 목록 */
        String titleEn,
        String oneLinerEn,
        String bodyEn,
        List<String> highlightsEn,
        /** 번역한 뒤 한국어 글이 바뀌었으면 true */
        boolean translationStale
) {

    static AdminArticleResponse from(CurationArticle article) {
        return new AdminArticleResponse(article.getId(), article.getGameId(), article.getStatus(), article.getKind(),
                article.getTitle(), article.getOneLiner(), article.getBody(), List.copyOf(article.getHighlights()),
                article.isSponsored(), article.getSponsorDisclosure(), article.getPublishedAt(),
                article.getTitleEn(), article.getOneLinerEn(), article.getBodyEn(),
                List.copyOf(article.getHighlightsEn()), article.isTranslationStale());
    }
}
