package com.horrorpond.curation.repository;

import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.catalog.domain.GameSource;
import com.horrorpond.catalog.domain.QGameMetricDaily;
import com.horrorpond.catalog.domain.TrendingOverride;
import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.catalog.domain.QGenre;
import com.horrorpond.curation.domain.ArticleKind;
import com.horrorpond.curation.domain.ArticleStatus;
import com.horrorpond.curation.domain.QAchievementGuide;
import com.horrorpond.curation.domain.QCurationArticle;
import com.horrorpond.curation.domain.QKoreanPatch;
import com.horrorpond.curation.domain.QPlayVideo;
import com.horrorpond.curation.domain.VideoKind;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
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
import java.util.Locale;
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
    static final char LIKE_ESCAPE = '!';
    private static final QCurationArticle article = QCurationArticle.curationArticle;
    private static final QPlayVideo playVideo = QPlayVideo.playVideo;
    private static final QAchievementGuide achievementGuide = QAchievementGuide.achievementGuide;
    private static final QKoreanPatch koreanPatch = QKoreanPatch.koreanPatch;

    /** 한국어 지원 필터의 언어 코드 (Game.languages) */
    static final String KOREAN = "ko";

    private final JPAQueryFactory queryFactory;

    public Page<CuratedGameRow> findVisible(PublicGameQuery query, Pageable pageable) {
        BooleanBuilder where = where(query);
        List<CuratedGameRow> rows = queryFactory
                .select(Projections.constructor(CuratedGameRow.class,
                        game.id, game.slug, game.title, game.titleEn, game.headerImageUrl, game.releaseDate,
                        game.releaseDateText, game.releaseDateTextEn, game.shortDescription, game.shortDescriptionEn,
                        game.shortDescriptionKoAuto,
                        game.comingSoon, game.coop, game.adult, game.reviewCount, game.tags,
                        game.languages, game.audioLanguages,
                        isArticle(ArticleKind.PICK), article.oneLiner, article.highlights,
                        article.oneLinerEn, article.highlightsEn,
                        article.sponsored.coalesce(false),
                        keeperVideoExists(),
                        JPAExpressions.selectOne().from(achievementGuide)
                                .where(achievementGuide.gameId.eq(game.id)).exists(),
                        game.source, isArticle(ArticleKind.REVIEW), game.free, game.endingType,
                        koreanPatchExists()))
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

    /** 목록과 같은 조건의 개수만 센다 (허브 통계용) */
    public long countVisible(PublicGameQuery query) {
        Long count = queryFactory
                .select(game.count())
                .from(game)
                .leftJoin(article).on(article.gameId.eq(game.id), article.status.eq(ArticleStatus.PUBLISHED))
                .where(where(query))
                .fetchOne();
        return count == null ? 0 : count;
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
            // 언어와 상관없이 한국어·영어 제목, 주소(slug, 공백은 -)에서 찾는다
            where.and(searchMatches(query.search()));
        }
        if (query.coop() != null) {
            where.and(game.coop.eq(query.coop()));
        }
        if (query.free()) {
            // 출시 전 게임은 사이트에 가격·무료 표시가 없으므로 무료 목록에서도 뺀다
            where.and(game.free.isTrue()).and(game.comingSoon.isFalse());
        }
        if (query.korean()) {
            // 공식 한국어 지원 또는 유저 한글패치가 있는 게임
            where.and(Expressions.booleanTemplate("array_contains({0}, {1})", game.languages,
                    Expressions.constant(KOREAN)).or(koreanPatchExists()));
        }
        if (query.picked()) {
            where.and(article.kind.eq(ArticleKind.PICK));
        }
        if (query.keeper()) {
            // 주인장 추천: 주인장 글(추천·후기)이 있거나 주인장 플레이 영상이 있는 게임
            where.and(article.kind.isNotNull()
                    .or(keeperVideoExists()));
        }
        if (query.release() != null) {
            // Steam은 출시 날짜만 주고 그날 정해진 시각에 풀리므로, 출시일이 오늘이어도 출시 예정이면 출시 전으로 본다.
            // 출시일이 지났는데 아직 출시 예정으로 남은 게임은 갱신이 늦은 것으로 보고 최근 출시로 본다 (프론트 isUpcoming과 같은 규칙)
            where.and(switch (query.release()) {
                case UPCOMING -> game.comingSoon.isTrue()
                        .and(game.releaseDate.isNull().or(game.releaseDate.goe(query.today())));
                case RECENT -> game.releaseDate.between(query.today().minusDays(ReleaseWindow.RECENT_DAYS),
                                query.today())
                        .and(game.comingSoon.isFalse().or(game.releaseDate.lt(query.today())));
                case TODAY -> game.releaseDate.eq(query.today());
                case TOMORROW -> game.releaseDate.eq(query.today().plusDays(1));
            });
        }
        if (query.genre() != null) {
            where.and(inGenre(query.genre()));
        }
        if (query.sort() == CuratedGameSort.TRENDING) {
            BooleanExpression horror = game.source.eq(GameSource.ITCH)
                    .or(Expressions.booleanTemplate("function('horror_in_top_tags', {0}, {1}) = true",
                            game.tags, Expressions.constant(CuratedGameSort.TRENDING_HORROR_TAG_RANK)));
            BooleanExpression pinned = game.trendingOverride.eq(TrendingOverride.PINNED);
            where.and(game.trendingOverride.isNull().or(game.trendingOverride.ne(TrendingOverride.EXCLUDED)))
                    .and(pinned.or(horror.and(recentReviewGrowth(query.today()).gt(0))));
        }
        return where;
    }

    /** 주인장 플레이 영상이 있는지 (다른 채널의 참고 영상은 세지 않는다) */
    private static BooleanExpression keeperVideoExists() {
        return JPAExpressions.selectOne().from(playVideo)
                .where(playVideo.gameId.eq(game.id), playVideo.kind.eq(VideoKind.KEEPER))
                .exists();
    }

    /** 유저 한글패치가 하나 이상 있는지 */
    private static BooleanExpression koreanPatchExists() {
        return JPAExpressions.selectOne().from(koreanPatch)
                .where(koreanPatch.gameId.eq(game.id))
                .exists();
    }

    /** 공개된 글이 그 종류면 true (글이 없으면 false) */
    private static BooleanExpression isArticle(ArticleKind kind) {
        return new CaseBuilder().when(article.kind.eq(kind)).then(true).otherwise(false);
    }

    /**
     * 한국어·영어 제목, 주소(slug)에서 찾는다. 공개 검색과 관리 화면 검색이 함께 쓴다.
     * 그대로 포함되는지 외에, 공백·문장부호(-)를 지우고 소문자로 맞춘 모양끼리도 비교한다
     * ("redorblue" ↔ "Red or Blue", "영덕 폐가" ↔ "영덕폐가").
     */
    static BooleanExpression searchMatches(String search) {
        String pattern = "%" + escapeLike(search) + "%";
        BooleanExpression matches = game.title.likeIgnoreCase(pattern, LIKE_ESCAPE)
                .or(game.titleEn.likeIgnoreCase(pattern, LIKE_ESCAPE))
                .or(game.slug.likeIgnoreCase(slugPattern(search), LIKE_ESCAPE));
        String compact = compactSearch(search);
        if (compact.isEmpty()) {
            return matches;
        }
        String compactPattern = "%" + escapeLike(compact) + "%";
        return matches.or(compacted(game.title).like(compactPattern, LIKE_ESCAPE))
                .or(compacted(game.titleEn).like(compactPattern, LIKE_ESCAPE))
                .or(compacted(game.slug).like(compactPattern, LIKE_ESCAPE));
    }

    /**
     * DB 쪽: 소문자로 바꾸고 공백·문장부호를 모두 지운다 (compactSearch와 같은 규칙, PostgreSQL regexp_replace).
     * Hibernate는 function()의 결과 타입을 모르므로 like 에 쓰려면 문자열로 cast 한다.
     */
    private static StringExpression compacted(StringExpression column) {
        return Expressions.stringTemplate(
                "cast(function('regexp_replace', lower({0}), '[[:space:][:punct:]]', '', 'g') as String)", column);
    }

    /**
     * 검색어 쪽: 소문자로 바꾸고 공백·문장부호를 지운다. 단 %, _ 는 남겨 둔다
     * (이스케이프되어 글자 그대로 찾으므로, 문장부호가 지워진 DB 값과는 맞지 않는다 = 와일드카드로 쓰이지 않는다).
     */
    static String compactSearch(String search) {
        return search.toLowerCase(Locale.ROOT).replaceAll("[\\p{IsWhite_Space}\\p{P}\\p{S}&&[^%_]]", "");
    }

    /** "red or blue" → "%red-or-blue%" (주소는 영어 이름을 - 로 이은 모양이다) */
    static String slugPattern(String search) {
        return "%" + escapeLike(search.strip().replaceAll("\\s+", "-")) + "%";
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
        OrderSpecifier<?>[] bySort = sortOrder(query);
        if (!query.keeper()) {
            return bySort;
        }
        // 주인장 추천 목록은 추천 글 → 플레이 후기 → 영상만 있는 게임 순, 그 안에서 고른 정렬
        OrderSpecifier<?>[] withPriority = new OrderSpecifier<?>[bySort.length + 1];
        withPriority[0] = new CaseBuilder()
                .when(article.kind.eq(ArticleKind.PICK)).then(0)
                .when(article.kind.eq(ArticleKind.REVIEW)).then(1)
                .otherwise(2).asc();
        System.arraycopy(bySort, 0, withPriority, 1, bySort.length);
        return withPriority;
    }

    private static OrderSpecifier<?>[] sortOrder(PublicGameQuery query) {
        return switch (query.sort()) {
            case LATEST -> query.picked()
                    ? new OrderSpecifier<?>[]{game.publishedAt.desc().nullsLast(), game.id.desc()}
                    : new OrderSpecifier<?>[]{game.createdAt.desc(), game.id.desc()};
            case RELEASE -> new OrderSpecifier<?>[]{game.releaseDate.desc().nullsLast(), game.id.desc()};
            case POPULAR -> new OrderSpecifier<?>[]{game.reviewCount.desc().nullsLast(), game.id.desc()};
            case TRENDING -> new OrderSpecifier<?>[]{
                    new CaseBuilder().when(game.trendingOverride.eq(TrendingOverride.PINNED)).then(0).otherwise(1).asc(),
                    game.trendingPinOrder.asc().nullsLast(),
                    recentReviewGrowth(query.today()).desc().nullsLast(), game.id.desc()};
        };
    }

    /**
     * 최근 7일 동안 늘어난 리뷰 수 = 지금 리뷰 수 − 7~14일 전 기록 중 가장 큰 값.
     * 그 기간 기록이 없으면, 출시한 지 7일이 안 된 게임은 0에서 세고 나머지는 null(목록에서 빠짐)이다.
     */
    private static NumberExpression<Integer> recentReviewGrowth(LocalDate today) {
        QGameMetricDaily metric = QGameMetricDaily.gameMetricDaily;
        LocalDate windowStart = today.minusDays(CuratedGameSort.TRENDING_WINDOW_DAYS);
        var baseline = JPAExpressions.select(metric.reviewCount.max())
                .from(metric)
                .where(metric.id.gameId.eq(game.id),
                        metric.id.capturedOn.between(today.minusDays(CuratedGameSort.TRENDING_BASELINE_MAX_DAYS),
                                windowStart));
        NumberExpression<Integer> newReleaseBaseline = new CaseBuilder()
                .when(game.releaseDate.goe(windowStart)).then(0)
                .otherwise(Expressions.nullExpression(Integer.class));
        return Expressions.numberTemplate(Integer.class, "coalesce({0}, 0) - coalesce({1}, {2})",
                game.reviewCount, baseline, newReleaseBaseline);
    }

    /**
     * @param visibility 노출 조건 (ExposurePolicy)
     * @param search     제목 검색어. 없으면 null
     * @param genre      장르 필터. 없으면 null
     * @param release    출시 시점 필터. 없으면 null
     * @param today      RECENT 계산 기준일
     * @param picked     true면 재일 추천(공개된 추천 글이 있는 게임, 후기 제외)만
     * @param korean     true면 한국어를 지원하거나 유저 한글패치가 있는 게임만
     * @param keeper     true면 주인장 추천(추천·후기 글 또는 주인장 플레이 영상이 있는 게임)만, 추천 → 후기 → 영상 순
     * @param free       true면 무료 게임만
     */
    public record PublicGameQuery(Predicate visibility, String search, GenreFilter genre, Boolean coop, ReleaseWindow release,
                                  LocalDate today, boolean picked, CuratedGameSort sort, boolean korean,
                                  boolean keeper, boolean free) {
    }

    public record GenreFilter(String slug, List<String> steamTags) {
    }

    public record CuratedGameRow(
            Long id,
            String slug,
            String title,
            String titleEn,
            String headerImageUrl,
            LocalDate releaseDate,
            String releaseDateText,
            String releaseDateTextEn,
            String shortDescription,
            String shortDescriptionEn,
            String shortDescriptionKoAuto,
            boolean comingSoon,
            boolean coop,
            boolean adult,
            Integer reviewCount,
            List<String> tags,
            List<String> languages,
            List<String> audioLanguages,
            boolean picked,
            String oneLiner,
            List<String> highlights,
            String oneLinerEn,
            List<String> highlightsEn,
            boolean sponsored,
            boolean hasPlayVideo,
            boolean hasAchievementGuide,
            GameSource source,
            boolean reviewed,
            boolean free,
            EndingType endingType,
            boolean hasKoreanPatch
    ) {
    }
}
