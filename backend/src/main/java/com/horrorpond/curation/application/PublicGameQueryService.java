package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.CuratedGameQueryRepository;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.CuratedGameRow;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.GenreFilter;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.PublicGameQuery;
import com.horrorpond.curation.repository.CuratedGameSort;
import com.horrorpond.curation.repository.CurationArticleRepository;
import com.horrorpond.curation.repository.ReleaseWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 공개 사이트용 조회. 노출 규칙(ExposurePolicy)을 만족하지 않으면 존재 여부도 드러내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicGameQueryService {

    /** "최근 출시" 계산 기준 (사이트 사용자 기준 날짜) */
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Seoul");

    private final CuratedGameQueryRepository curatedGameQueryRepository;
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final CurationArticleRepository articleRepository;
    private final ExposurePolicy exposurePolicy;
    private final Clock clock;

    public Page<GameSummaryResponse> list(String genreSlug, Boolean coop, ReleaseWindow release, boolean picked,
                                          CuratedGameSort sort, Pageable pageable) {
        List<Genre> genres = genreRepository.findAllByOrderByDisplayOrderAsc();
        GenreFilter genreFilter = genreSlug == null || genreSlug.isBlank() ? null : genres.stream()
                .filter(genre -> genre.getSlug().equals(genreSlug))
                .findFirst()
                .map(genre -> new GenreFilter(genre.getSlug(), genre.getSteamTags()))
                .orElse(new GenreFilter(genreSlug, List.of()));
        PublicGameQuery query = new PublicGameQuery(exposurePolicy.visible(QGame.game), genreFilter, coop, release,
                LocalDate.now(clock.withZone(SITE_ZONE)), picked, sort);

        Page<CuratedGameRow> rows = curatedGameQueryRepository.findVisible(query, pageable);
        Map<Long, List<String>> curatorGenres = curatedGameQueryRepository.findCuratorGenreSlugs(
                rows.map(CuratedGameRow::id).getContent());
        GenreResolver resolver = new GenreResolver(genres);
        return rows.map(row -> GameSummaryResponse.of(row,
                resolver.resolve(curatorGenres.getOrDefault(row.id(), List.of()), row.tags())));
    }

    /**
     * 컬렉션(미디어/개발사/장르/상점 링크)은 default_batch_fetch_size로 읽어 개수와 무관하게 쿼리 수가 일정하다.
     */
    public GameDetailResponse detail(String slug) {
        Game game = gameRepository.findBySlug(slug)
                .filter(exposurePolicy::isVisible)
                .orElseThrow(() -> new NotFoundException("Game not found: " + slug));
        CurationArticle article = articleRepository.findByGameId(game.getId())
                .filter(CurationArticle::isPublished)
                .orElse(null);
        GenreResolver resolver = new GenreResolver(genreRepository.findAllByOrderByDisplayOrderAsc());
        List<String> curatorSlugs = game.getGenres().stream().map(Genre::getSlug).toList();
        return GameDetailResponse.of(game, resolver.resolve(curatorSlugs, game.getTags()), article);
    }
}
