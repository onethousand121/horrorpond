package com.horrorpond.curation.repository;

import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.QCurationArticle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 검토 대기열. 글이 없는 게임도 보여야 하므로 article은 LEFT JOIN 한다.
 * sameTitleCount: 제목이 같은(대소문자 무시) 다른 게임 수. 원작/리마스터, 본편/체험판처럼 Steam에 따로 등록된
 * 같은 이름의 게임을 큐레이터가 구분할 수 있게 한다.
 */
@Repository
@RequiredArgsConstructor
public class AdminGameQueryRepository {

    private static final QGame game = QGame.game;
    private static final QGame sameTitle = new QGame("sameTitle");
    private static final QCurationArticle article = QCurationArticle.curationArticle;

    private final JPAQueryFactory queryFactory;

    public Page<AdminGameRow> search(GameStatus status, String titleQuery, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        if (status != null) {
            where.and(game.status.eq(status));
        }
        if (titleQuery != null && !titleQuery.isBlank()) {
            where.and(game.title.containsIgnoreCase(titleQuery.strip()));
        }
        List<AdminGameRow> rows = queryFactory
                .select(Projections.constructor(AdminGameRow.class,
                        game.id, game.source, game.externalId, game.slug, game.title, game.headerImageUrl, game.releaseDate,
                        game.comingSoon, game.coop, game.reviewCount, game.adult, game.status, article.status,
                        JPAExpressions.select(sameTitle.count())
                                .from(sameTitle)
                                .where(sameTitle.title.lower().eq(game.title.lower()), sameTitle.id.ne(game.id))))
                .from(game)
                .leftJoin(article).on(article.gameId.eq(game.id))
                .where(where)
                .orderBy(game.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return PageableExecutionUtils.getPage(rows, pageable, () -> queryFactory
                .select(game.count())
                .from(game)
                .where(where)
                .fetchOne());
    }

    public record AdminGameRow(
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
            ArticleStatus articleStatus,
            long sameTitleCount
    ) {
    }
}
