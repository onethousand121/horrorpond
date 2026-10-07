package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.AutoExposure;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameStatus;
import com.horrorpond.catalog.domain.QGame;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 공개 사이트 노출 규칙. 목록(쿼리 조건)과 상세(엔티티 판정)가 같은 규칙을 쓰도록 한 곳에 둔다.
 * 규칙 자체는 {@link Game#isPubliclyVisible(AutoExposure, LocalDate)}, {@link AutoExposure} 참고.
 * "오늘"은 사이트 기준(한국) 날짜다.
 */
@Component
public class ExposurePolicy {

    static final ZoneId SITE_ZONE = ZoneId.of("Asia/Seoul");

    private final AutoExposure rule;
    private final Clock clock;

    public ExposurePolicy(@Value("${curation.auto-exposure.min-reviews:10}") int minReviews,
                          @Value("${curation.auto-exposure.new-release-days:10}") int newReleaseDays,
                          Clock clock) {
        this.rule = new AutoExposure(minReviews, newReleaseDays);
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(SITE_ZONE));
    }

    public boolean isVisible(Game game) {
        return game.isPubliclyVisible(rule, today());
    }

    public boolean isVisible(GameStatus status, boolean adult, boolean comingSoon, Integer reviewCount,
                             LocalDate releaseDate) {
        return Game.isPubliclyVisible(status, adult, comingSoon, reviewCount, releaseDate, rule, today());
    }

    public BooleanExpression visible(QGame game) {
        LocalDate today = today();
        BooleanExpression autoExposed = game.status.eq(GameStatus.CANDIDATE)
                .and(game.adult.isFalse())
                .and(game.comingSoon.isTrue()
                        .or(game.releaseDate.between(rule.newReleaseSince(today), today))
                        .or(game.reviewCount.goe(rule.minReviews())));
        return game.status.eq(GameStatus.PUBLISHED).or(autoExposed);
    }
}
