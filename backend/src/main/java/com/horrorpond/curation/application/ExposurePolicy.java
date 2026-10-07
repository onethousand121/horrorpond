package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.QGame;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 공개 사이트 노출 규칙. 목록(쿼리 조건)과 상세(엔티티 판정)가 같은 규칙을 쓰도록 한 곳에 둔다.
 * 규칙 자체는 {@link Game#isPubliclyVisible(int)} 참고.
 */
@Component
public class ExposurePolicy {

    private final int minReviews;

    public ExposurePolicy(@Value("${curation.auto-exposure.min-reviews:10}") int minReviews) {
        this.minReviews = minReviews;
    }

    public boolean isVisible(Game game) {
        return game.isPubliclyVisible(minReviews);
    }

    public boolean isVisible(GameStatus status, boolean adult, boolean comingSoon, Integer reviewCount) {
        return Game.isPubliclyVisible(status, adult, comingSoon, reviewCount, minReviews);
    }

    public BooleanExpression visible(QGame game) {
        BooleanExpression autoExposed = game.status.eq(GameStatus.CANDIDATE)
                .and(game.adult.isFalse())
                .and(game.comingSoon.isTrue().or(game.reviewCount.goe(minReviews)));
        return game.status.eq(GameStatus.PUBLISHED).or(autoExposed);
    }
}
