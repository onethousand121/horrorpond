package com.horrorpond.curation.repository;

import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.catalog.domain.QGenre;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.QCurationArticle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공개 게임 목록 조회. "공개"는 game.status = PUBLISHED 이면서 article.status = PUBLISHED 인 것.
 * 컬렉션 fetch join은 메모리 페이징을 일으키므로 쓰지 않고, 스칼라 projection 후 장르를 따로 한 번에 읽는다.
 */
@Repository
@RequiredArgsConstructor
public class CuratedGameQueryRepository {

    private static final QGame game = QGame.game;
    private static final QCurationArticle article = QCurationArticle.curationArticle;

    private final JPAQueryFactory queryFactory;

    public Page<CuratedGameRow> findPublished(String genreSlug, Boolean coop, CuratedGameSort sort,
                                              Pageable pageable) {
        BooleanBuilder where = publishedWhere(genreSlug, coop);
        List<CuratedGameRow> rows = queryFactory
                .select(Projections.constructor(CuratedGameRow.class,
                        game.id, game.slug, game.title, game.headerImageUrl, game.releaseDate,
                        game.comingSoon, game.coop,
                        article.oneLiner, article.highlights, article.sponsored))
                .from(game)
                .join(article).on(article.gameId.eq(game.id))
                .where(where)
                .orderBy(orderBy(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return PageableExecutionUtils.getPage(rows, pageable, () -> queryFactory
                .select(game.count())
                .from(game)
                .join(article).on(article.gameId.eq(game.id))
                .where(where)
                .fetchOne());
    }

    /**
     * 목록에 나온 게임들의 장르를 IN 쿼리 1번으로 읽는다. 결과는 displayOrder 순.
     */
    public Map<Long, List<GenreRow>> findGenresByGameIds(Collection<Long> gameIds) {
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        QGenre genre = QGenre.genre;
        List<Tuple> tuples = queryFactory
                .select(game.id, genre.slug, genre.name)
                .from(game)
                .join(game.genres, genre)
                .where(game.id.in(gameIds))
                .orderBy(genre.displayOrder.asc(), genre.id.asc())
                .fetch();
        return tuples.stream().collect(Collectors.groupingBy(
                tuple -> tuple.get(game.id),
                LinkedHashMap::new,
                Collectors.mapping(tuple -> new GenreRow(tuple.get(genre.slug), tuple.get(genre.name)),
                        Collectors.toList())));
    }

    /**
     * 장르 필터는 JOIN이 아니라 EXISTS 서브쿼리로 건다 (게임 행이 장르 수만큼 중복되지 않게).
     * 존재하지 않는 장르 slug면 결과가 비게 된다.
     */
    private static BooleanBuilder publishedWhere(String genreSlug, Boolean coop) {
        BooleanBuilder where = new BooleanBuilder()
                .and(game.status.eq(GameStatus.PUBLISHED))
                .and(article.status.eq(ArticleStatus.PUBLISHED));
        if (coop != null) {
            where.and(game.coop.eq(coop));
        }
        if (genreSlug != null && !genreSlug.isBlank()) {
            QGame tagged = new QGame("taggedGame");
            QGenre filterGenre = new QGenre("filterGenre");
            where.and(JPAExpressions.selectOne()
                    .from(tagged)
                    .join(tagged.genres, filterGenre)
                    .where(tagged.id.eq(game.id), filterGenre.slug.eq(genreSlug))
                    .exists());
        }
        return where;
    }

    private static OrderSpecifier<?>[] orderBy(CuratedGameSort sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier<?>[]{game.publishedAt.desc(), game.id.desc()};
            case RELEASE -> new OrderSpecifier<?>[]{game.releaseDate.desc().nullsLast(), game.id.desc()};
        };
    }

    public record CuratedGameRow(
            Long id,
            String slug,
            String title,
            String headerImageUrl,
            LocalDate releaseDate,
            boolean comingSoon,
            boolean coop,
            String oneLiner,
            List<String> highlights,
            boolean sponsored
    ) {
    }

    public record GenreRow(String slug, String name) {
    }
}
