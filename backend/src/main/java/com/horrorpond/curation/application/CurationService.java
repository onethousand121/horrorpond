package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.domain.TrendingOverride;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.ArticleKind;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.AdminGameQueryRepository;
import com.horrorpond.curation.repository.CurationArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    private final GameGuideService gameGuideService;

    @Transactional(readOnly = true)
    public Page<AdminGameResponse> search(GameStatus status, String titleQuery, Pageable pageable) {
        return adminGameQueryRepository.search(status, titleQuery, pageable).map(row -> AdminGameResponse.from(row, exposurePolicy));
    }

    @Transactional(readOnly = true)
    public AdminGameDetailResponse getDetail(Long gameId) {
        Game game = findGame(gameId);
        return AdminGameDetailResponse.of(game, articleRepository.findByGameId(gameId).orElse(null), exposurePolicy,
                gameGuideService.playVideos(gameId), gameGuideService.achievements(gameId),
                gameGuideService.referenceVideos(gameId), gameGuideService.endings(gameId),
                gameGuideService.koreanPatches(gameId));
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
                    existing.edit(command.kind(), command.title(), command.oneLiner(), command.body(),
                            command.highlights());
                    return existing;
                })
                .orElseGet(() -> articleRepository.save(CurationArticle.draft(gameId, command.kind(), command.title(),
                        command.oneLiner(), command.body(), command.highlights())));
        if (command.sponsored()) {
            article.markSponsored(command.sponsorDisclosure());
        } else {
            article.clearSponsored();
        }
        return AdminArticleResponse.from(article);
    }

    /**
     * 추천 내리기: 글은 초안으로 남긴다.
     */
    public AdminGameResponse unpublishArticle(Long gameId) {
        Game game = findGame(gameId);
        CurationArticle article = articleRepository.findByGameId(gameId)
                .orElseThrow(() -> new NotFoundException("Article not found: gameId=" + gameId));
        article.unpublish();
        return AdminGameResponse.of(game, article, exposurePolicy);
    }

    /**
     * 글 삭제: 공개 여부와 상관없이 지운다. 게임의 노출 상태(고정 노출 등)는 그대로다.
     */
    public AdminGameResponse deleteArticle(Long gameId) {
        Game game = findGame(gameId);
        articleRepository.findByGameId(gameId).ifPresent(articleRepository::delete);
        return AdminGameResponse.of(game, null, exposurePolicy);
    }

    /**
     * "지금 뜨는" 고정/제외. null이면 자동 기준으로 되돌린다.
     */
    public AdminGameDetailResponse changeTrendingOverride(Long gameId, TrendingOverride override, boolean auto) {
        findGame(gameId).changeTrendingOverride(override, auto);
        return getDetail(gameId);
    }

    /**
     * 상점 정보로 게임을 찾는다 (매주 조사 반영용). Steam은 appid, itch.io는 상점 주소.
     */
    @Transactional(readOnly = true)
    public AdminGameDetailResponse lookup(Integer steamAppId, String itchUrl) {
        Game game;
        if (steamAppId != null) {
            game = gameRepository.findBySourceAndExternalId(GameSource.STEAM, String.valueOf(steamAppId))
                    .orElseThrow(() -> new NotFoundException("Steam game not found: " + steamAppId));
        } else if (itchUrl != null && !itchUrl.isBlank()) {
            game = gameRepository.findByStoreLink(Store.ITCH, itchUrl.strip()).stream().findFirst()
                    .orElseThrow(() -> new NotFoundException("itch.io game not found: " + itchUrl));
        } else {
            throw new DomainValidationException("steamAppId or itchUrl is required");
        }
        return getDetail(game.getId());
    }

    /** "지금 뜨는"에 고정된 게임 (자동 고정 여부 포함). 사이트와 같은 순서: 정한 순서 → 순서 없는 게임 */
    @Transactional(readOnly = true)
    public List<TrendingPin> trendingPins() {
        return gameRepository.findByTrendingOverrideOrderByIdAsc(TrendingOverride.PINNED).stream()
                .sorted(Comparator.comparing(Game::getTrendingPinOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(game -> new TrendingPin(game.getId(), game.getTitle(), game.isTrendingAuto(),
                        game.getTrendingPinOrder()))
                .toList();
    }

    /**
     * 고정 게임의 순서를 목록 순서대로 정한다. 목록에 없는 고정 게임은 순서를 지운다 (정한 게임들 뒤에 나온다).
     */
    public List<TrendingPin> reorderTrendingPins(List<Long> gameIds) {
        Map<Long, Game> pinned = gameRepository.findByTrendingOverrideOrderByIdAsc(TrendingOverride.PINNED).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        if (new HashSet<>(gameIds).size() != gameIds.size()) {
            throw new DomainValidationException("gameIds must not repeat");
        }
        for (Long id : gameIds) {
            if (!pinned.containsKey(id)) {
                throw new DomainValidationException("not a pinned game: " + id);
            }
        }
        pinned.values().forEach(game -> game.changeTrendingPinOrder(null));
        for (int i = 0; i < gameIds.size(); i++) {
            pinned.get(gameIds.get(i)).changeTrendingPinOrder(i);
        }
        return trendingPins();
    }

    /** @param order 고정 게임끼리의 순서 (작을수록 앞). 정하지 않았으면 null */
    public record TrendingPin(Long id, String title, boolean auto, Integer order) {
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

    public record ArticleCommand(ArticleKind kind, String title, String oneLiner, String body,
                                 List<String> highlights, boolean sponsored, String sponsorDisclosure) {
    }
}
