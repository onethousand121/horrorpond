package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Store;
import com.horrorpond.catalog.repository.GameMetricDailyRepository;
import com.horrorpond.catalog.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 리뷰 수 1건을 1트랜잭션으로 반영한다: 게임의 현재 리뷰 수(노출 판정에 쓰임)와 그날의 기록.
 */
@Component
@RequiredArgsConstructor
public class ReviewMetricsWriter {

    private final GameRepository gameRepository;
    private final GameMetricDailyRepository metricRepository;
    private final Clock clock;

    @Transactional
    public void record(long gameId, int reviewCount, LocalDate capturedOn) {
        gameRepository.findById(gameId).ifPresent(game -> {
            game.updateReviewCount(reviewCount);
            metricRepository.upsert(gameId, Store.STEAM.name(), capturedOn, reviewCount, clock.instant());
        });
    }
}
