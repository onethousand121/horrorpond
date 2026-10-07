package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.AdminGameQueryRepository;
import com.horrorpond.curation.repository.CurationArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 큐레이터 작업: 검토 대기열 조회, 큐레이터 소유 필드 수정, 글 작성, 숨김.
 * 공개는 {@link PublishingService}가 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CurationService {

    private final AdminGameQueryRepository adminGameQueryRepository;
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final CurationArticleRepository articleRepository;
    private final ExposurePolicy exposurePolicy;

    @Transactional(readOnly = true)
    public Page<AdminGameResponse> search(GameStatus status, String titleQuery, Pageable pageable) {
        return adminGameQueryRepository.search(status, titleQuery, pageable).map(row -> AdminGameResponse.from(row, exposurePolicy));
    }

    @Transactional(readOnly = true)
    public AdminGameDetailResponse getDetail(Long gameId) {
        Game game = findGame(gameId);
        return AdminGameDetailResponse.of(game, articleRepository.findByGameId(gameId).orElse(null), exposurePolicy);
    }

    /**
     * @param coop null이면 변경하지 않는다. STEAM 게임의 coop은 Steam 데이터가 소유하므로 지정하면 400.
     */
    public AdminGameResponse updateCuration(Long gameId, String slug, List<String> genreSlugs, Boolean coop) {
        Game game = findGame(gameId);
        if (coop != null && game.getSource() == GameSource.STEAM) {
            throw new DomainValidationException("coop of STEAM games is derived from Steam categories");
        }
        if (!slug.equals(game.getSlug()) && gameRepository.existsBySlug(slug)) {
            throw new DomainStateException("Slug already in use: " + slug);
        }
        Set<Genre> genres = resolveGenres(genreSlugs);

        game.changeSlug(slug);
        game.replaceGenres(genres);
        if (coop != null) {
            game.changeCoop(coop);
        }
        return AdminGameResponse.of(game, articleRepository.findByGameId(gameId).orElse(null), exposurePolicy);
    }

    /**
     * 글이 없으면 초안을 만들고, 있으면 수정한다. 공개 상태인 글을 수정해도 공개 상태는 유지된다.
     */
    public AdminArticleResponse upsertArticle(Long gameId, ArticleCommand command) {
        findGame(gameId);
        CurationArticle article = articleRepository.findByGameId(gameId)
                .map(existing -> {
                    existing.edit(command.title(), command.oneLiner(), command.body(), command.highlights());
                    return existing;
                })
                .orElseGet(() -> articleRepository.save(CurationArticle.draft(gameId, command.title(),
                        command.oneLiner(), command.body(), command.highlights())));
        if (command.sponsored()) {
            article.markSponsored(command.sponsorDisclosure());
        } else {
            article.clearSponsored();
        }
        return AdminArticleResponse.from(article);
    }

    public AdminGameResponse hide(Long gameId) {
        Game game = findGame(gameId);
        game.hide();
        return AdminGameResponse.of(game, articleRepository.findByGameId(gameId).orElse(null), exposurePolicy);
    }

    public AdminGameResponse unhide(Long gameId) {
        Game game = findGame(gameId);
        game.unhide();
        return AdminGameResponse.of(game, articleRepository.findByGameId(gameId).orElse(null), exposurePolicy);
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
    }

    private Set<Genre> resolveGenres(List<String> genreSlugs) {
        Set<String> requested = new LinkedHashSet<>(genreSlugs);
        List<Genre> found = genreRepository.findBySlugIn(requested);
        if (found.size() != requested.size()) {
            Set<String> foundSlugs = found.stream().map(Genre::getSlug).collect(Collectors.toSet());
            List<String> missing = requested.stream().filter(slug -> !foundSlugs.contains(slug)).toList();
            throw new DomainValidationException("Unknown genre slugs: " + missing);
        }
        return new HashSet<>(found);
    }

    public record ArticleCommand(String title, String oneLiner, String body, List<String> highlights,
                                 boolean sponsored, String sponsorDisclosure) {
    }
}
