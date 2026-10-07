package com.horrorpond.curation.repository;

import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.catalog.domain.QGenre;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.QCurationArticle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
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
 * 공개 게임 목록 조회. 노출 조건은 호출하는 쪽(ExposurePolicy)이 넘겨준다.
 * 큐레이터 글은 선택이라 공개된 글만 LEFT JOIN 한다(재일 추천 = 공개된 글이 있는 게임).
 * 컬렉션 fetch join은 메모리 페이징을 일으키므로 쓰지 않고, 스칼라 projection 후 장르를 따로 한 번에 읽는다.
 */
@Repository
@RequiredArgsConstructor
public class CuratedGameQueryRepository {

    private static final QGame game = QGame.game;
    private static final char LIKE_ESCAPE = '!';
    private static final QCurationArticle article = QCurationArticle.curationArticle;

    private final JPAQueryFactory queryFactory;

    public Page<CuratedGameRow> findVisible(PublicGameQuery query, Pageable pageable) {
        BooleanBuilder where = where(query);
        List<CuratedGameRow> rows = queryFactory
                .select(Projections.constructor(CuratedGameRow.class,
                        game.id, game.slug, game.title, game.headerImageUrl, game.releaseDate,
                        game.releaseDateText, game.shortDescription, game.comingSoon, game.coop, game.reviewCount, game.tags,
                        article.id.isNotNull(), article.oneLiner, article.highlights, article.sponsored.coalesce(false)))
                .from(game)
                .leftJoin(article).on(article.gameId.eq(game.id), article.status.eq(ArticleStatus.PUBLISHED))
                .where(where)
                .orderBy(orderBy(query))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return PageableExecutionUtils.getPage(rows, pageable, () -> queryFactory
                .select(game.count())
                .from(game)
                .leftJoin(article).on(article.gameId.eq(game.id), article.status.eq(ArticleStatus.PUBLISHED))
                .where(where)
                .fetchOne());
    }

    /**
     * 목록에 나온 게임들에 큐레이터가 붙인 장르를 IN 쿼리 1번으로 읽는다.
     */
    public Map<Long, List<String>> findCuratorGenreSlugs(Collection<Long> gameIds) {
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        QGenre genre = QGenre.genre;
        List<Tuple> tuples = queryFactory
                .select(game.id, genre.slug)
                .from(game)
                .join(game.genres, genre)
                .where(game.id.in(gameIds))
                .fetch();
        return tuples.stream().collect(Collectors.groupingBy(
                tuple -> tuple.get(game.id),
                LinkedHashMap::new,
                Collectors.mapping(tuple -> tuple.get(genre.slug), Collectors.toList())));
    }

    private static BooleanBuilder where(PublicGameQuery query) {
        BooleanBuilder where = new BooleanBuilder(query.visibility());
        if (query.search() != null) {
            where.and(game.title.likeIgnoreCase("%" + escapeLike(query.search()) + "%", LIKE_ESCAPE));
        }
        if (query.coop() != null) {
            where.and(game.coop.eq(query.coop()));
        }
        if (query.picked()) {
            where.and(article.id.isNotNull());
        }
        if (query.release() != null) {
            where.and(switch (query.release()) {
                case UPCOMING -> game.comingSoon.isTrue();
                case RECENT -> game.comingSoon.isFalse()
                        .and(game.releaseDate.between(query.today().minusDays(ReleaseWindow.RECENT_DAYS), query.today()));
            });
        }
        if (query.genre() != null) {
            where.and(inGenre(query.genre()));
        }
        return where;
    }

    /** 검색어의 %, _ 를 글자 그대로 찾도록 이스케이프한다. */
    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * 큐레이터가 붙인 장르이거나, SteamSpy 태그가 장르의 태그와 하나라도 겹치면 그 장르로 본다.
     * 장르 필터는 JOIN이 아니라 EXISTS 서브쿼리로 건다 (게임 행이 장르 수만큼 중복되지 않게).
     */
    private static BooleanExpression inGenre(GenreFilter genre) {
        QGame tagged = new QGame("taggedGame");
        QGenre filterGenre = new QGenre("filterGenre");
        BooleanExpression curated = JPAExpressions.selectOne()
                .from(tagged)
                .join(tagged.genres, filterGenre)
                .where(tagged.id.eq(game.id), filterGenre.slug.eq(genre.slug()))
                .exists();
        if (genre.steamTags().isEmpty()) {
            return curated;
        }
        BooleanExpression byTags = Expressions.booleanTemplate("array_intersects({0}, {1})",
                game.tags, Expressions.constant(genre.steamTags().toArray(String[]::new)));
        return curated.or(byTags);
    }

    private static OrderSpecifier<?>[] orderBy(PublicGameQuery query) {
        if (query.release() == ReleaseWindow.UPCOMING) {
            return new OrderSpecifier<?>[]{game.releaseDate.asc().nullsLast(), game.id.desc()};
        }
        return switch (query.sort()) {
            case LATEST -> query.picked()
                    ? new OrderSpecifier<?>[]{game.publishedAt.desc().nullsLast(), game.id.desc()}
                    : new OrderSpecifier<?>[]{game.createdAt.desc(), game.id.desc()};
            case RELEASE -> new OrderSpecifier<?>[]{game.releaseDate.desc().nullsLast(), game.id.desc()};
            case POPULAR -> new OrderSpecifier<?>[]{game.reviewCount.desc().nullsLast(), game.id.desc()};
        };
    }

    /**
     * @param visibility 노출 조건 (ExposurePolicy)
     * @param search     제목 검색어. 없으면 null
     * @param genre      장르 필터. 없으면 null
     * @param release    출시 시점 필터. 없으면 null
     * @param today      RECENT 계산 기준일
     * @param picked     true면 재일 추천(공개된 글이 있는 게임)만
     */
    public record PublicGameQuery(Predicate visibility, String search, GenreFilter genre, Boolean coop, ReleaseWindow release,
                                  LocalDate today, boolean picked, CuratedGameSort sort) {
    }

    public record GenreFilter(String slug, List<String> steamTags) {
    }

    public record CuratedGameRow(
            Long id,
            String slug,
            String title,
            String headerImageUrl,
            LocalDate releaseDate,
            String releaseDateText,
            String shortDescription,
            boolean comingSoon,
            boolean coop,
            Integer reviewCount,
            List<String> tags,
            boolean picked,
            String oneLiner,
            List<String> highlights,
            boolean sponsored
    ) {
    }
}
