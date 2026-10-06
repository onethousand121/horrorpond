package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.CuratedGameQueryRepository;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.CuratedGameRow;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.GenreRow;
import com.horrorpond.curation.repository.CuratedGameSort;
import com.horrorpond.curation.repository.CurationArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 공개 사이트용 조회. 공개 조건(게임·글 모두 PUBLISHED)을 만족하지 않으면 존재 여부도 드러내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicGameQueryService {

    private final CuratedGameQueryRepository curatedGameQueryRepository;
    private final GameRepository gameRepository;
    private final CurationArticleRepository articleRepository;

    public Page<GameSummaryResponse> list(String genreSlug, Boolean coop, CuratedGameSort sort, Pageable pageable) {
        Page<CuratedGameRow> rows = curatedGameQueryRepository.findPublished(genreSlug, coop, sort, pageable);
        Map<Long, List<GenreRow>> genres = curatedGameQueryRepository.findGenresByGameIds(
                rows.map(CuratedGameRow::id).getContent());
        return rows.map(row -> GameSummaryResponse.of(row, genres.getOrDefault(row.id(), List.of())));
    }

    /**
     * 컬렉션(미디어/개발사/장르/상점 링크)은 default_batch_fetch_size로 읽어 개수와 무관하게 쿼리 수가 일정하다.
     */
    public GameDetailResponse detail(String slug) {
        Game game = gameRepository.findBySlugAndStatus(slug, GameStatus.PUBLISHED)
                .orElseThrow(() -> notFound(slug));
        CurationArticle article = articleRepository.findByGameId(game.getId())
                .filter(CurationArticle::isPublished)
                .orElseThrow(() -> notFound(slug));
        return GameDetailResponse.of(game, article);
    }

    private static NotFoundException notFound(String slug) {
        return new NotFoundException("Game not found: " + slug);
    }
}
