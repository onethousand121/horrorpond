package com.horrorpond.catalog.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@Getter
@Entity
@Table(name = "game_developer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"game", "developer"})
public class GameDeveloper {

    @EmbeddedId
    private GameDeveloperId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    @MapsId("developerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "developer_id")
    private Developer developer;

    static GameDeveloper of(Game game, Developer developer, DeveloperRole role) {
        GameDeveloper credit = new GameDeveloper();
        credit.id = GameDeveloperId.of(game.getId(), developer.getId(), role);
        credit.game = game;
        credit.developer = developer;
        return credit;
    }

    public DeveloperRole getRole() {
        return id.getRole();
    }

    boolean matches(Developer other, DeveloperRole role) {
        return getRole() == role && isSameDeveloper(other);
    }

    private boolean isSameDeveloper(Developer other) {
        if (developer == other) {
            return true;
        }
        return developer.getId() != null && Objects.equals(developer.getId(), other.getId());
    }
}
