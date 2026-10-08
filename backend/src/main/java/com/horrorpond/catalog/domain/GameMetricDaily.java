package com.horrorpond.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.ZoneId;

/**
 * 게임별·상점별 하루 1건 리뷰 수 기록. 트렌드(최근 며칠 사이 증가량) 계산의 원본이다.
 * 같은 날 여러 번 기록하면 마지막 값이 남는다(저장은 {@code GameMetricDailyRepository#upsert}).
 * Game과는 다른 애그리거트라 gameId로만 참조한다.
 */
@Getter
@Entity
@Table(name = "game_metric_daily")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class GameMetricDaily {

    /** 기록 날짜(captured_on)의 기준. 사이트의 "오늘"과 같은 한국 시간 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @EmbeddedId
    private GameMetricDailyId id;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
