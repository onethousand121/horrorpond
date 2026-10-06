package com.horrorpond.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Entity
@Table(name = "game_media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "game")
public class GameMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private int sortOrder;

    static GameMedia of(Game game, MediaType type, String url, String thumbnailUrl, int sortOrder) {
        GameMedia media = new GameMedia();
        media.game = game;
        media.type = type;
        media.url = url;
        media.thumbnailUrl = thumbnailUrl;
        media.sortOrder = sortOrder;
        return media;
    }
}
