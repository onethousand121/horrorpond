package com.horrorpond.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@ToString
public class GameMetricDailyId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", length = 20)
    private Store store;

    @Column(name = "captured_on")
    private LocalDate capturedOn;

    public static GameMetricDailyId of(Long gameId, Store store, LocalDate capturedOn) {
        GameMetricDailyId id = new GameMetricDailyId();
        id.gameId = gameId;
        id.store = store;
        id.capturedOn = capturedOn;
        return id;
    }
}
