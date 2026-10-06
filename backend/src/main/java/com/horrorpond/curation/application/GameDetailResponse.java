package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.DeveloperRole;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameDeveloper;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.MediaType;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.curation.application.GameSummaryResponse.GenreSummary;
import com.horrorpond.curation.domain.CurationArticle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record GameDetailResponse(
        String slug,
        String title,
        String shortDescription,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        boolean comingSoon,
        boolean coop,
        List<GenreSummary> genres,
        List<DeveloperCredit> developers,
        List<Media> media,
        List<StoreLinkResponse> storeLinks,
        Article article
) {

    static GameDetailResponse of(Game game, CurationArticle article) {
        return new GameDetailResponse(
                game.getSlug(), game.getTitle(), game.getShortDescription(), game.getHeaderImageUrl(),
                game.getReleaseDate(), game.getReleaseDateText(), game.isComingSoon(), game.isCoop(),
                game.getGenres().stream()
                        .sorted(Comparator.comparingInt(Genre::getDisplayOrder).thenComparing(Genre::getSlug))
                        .map(genre -> new GenreSummary(genre.getSlug(), genre.getName()))
                        .toList(),
                game.getDevelopers().stream()
                        .sorted(Comparator.comparing(GameDeveloper::getRole)
                                .thenComparing(credit -> credit.getDeveloper().getName()))
                        .map(DeveloperCredit::from)
                        .toList(),
                game.getMedia().stream()
                        .map(media -> new Media(media.getType(), media.getUrl(), media.getThumbnailUrl()))
                        .toList(),
                game.getStoreLinks().stream()
                        .map(link -> new StoreLinkResponse(link.getStore(), link.getUrl()))
                        .toList(),
                Article.from(article));
    }

    public record DeveloperCredit(String name, String slug, DeveloperRole role) {

        static DeveloperCredit from(GameDeveloper credit) {
            return new DeveloperCredit(credit.getDeveloper().getName(), credit.getDeveloper().getSlug(),
                    credit.getRole());
        }
    }

    public record Media(MediaType type, String url, String thumbnailUrl) {
    }

    public record StoreLinkResponse(Store store, String url) {
    }

    /**
     * @param body markdown 원문. 렌더링은 프론트엔드가 한다.
     */
    public record Article(String title, String oneLiner, String body, List<String> highlights,
                          boolean sponsored, String sponsorDisclosure, Instant publishedAt) {

        static Article from(CurationArticle article) {
            return new Article(article.getTitle(), article.getOneLiner(), article.getBody(),
                    List.copyOf(article.getHighlights()), article.isSponsored(), article.getSponsorDisclosure(),
                    article.getPublishedAt());
        }
    }
}
