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

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@ToString
public class GameDeveloperId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "developer_id")
    private Long developerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private DeveloperRole role;

    static GameDeveloperId of(Long gameId, Long developerId, DeveloperRole role) {
        GameDeveloperId id = new GameDeveloperId();
        id.gameId = gameId;
        id.developerId = developerId;
        id.role = role;
        return id;
    }
}
