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
@Table(name = "store_link")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "game")
public class StoreLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Store store;

    @Column(nullable = false, length = 500)
    private String url;

    static StoreLink of(Game game, Store store, String url) {
        StoreLink link = new StoreLink();
        link.game = game;
        link.store = store;
        link.url = url;
        return link;
    }

    void changeUrl(String url) {
        this.url = url;
    }
}
