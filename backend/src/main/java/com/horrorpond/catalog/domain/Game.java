package com.horrorpond.catalog.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Entity
@Table(name = "game")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"media", "storeLinks", "genres", "developers"})
public class Game extends BaseTimeEntity {

    private static final String STEAM_STORE_URL = "https://store.steampowered.com/app/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSource source;

    @Column(length = 50)
    private String externalId;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String shortDescription;

    @Column(length = 500)
    private String headerImageUrl;

    private LocalDate releaseDate;

    @Column(length = 100)
    private String releaseDateText;

    @Column(nullable = false)
    private boolean comingSoon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    private Instant publishedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<GameMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreLink> storeLinks = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "game_genre",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameDeveloper> developers = new ArrayList<>();

    // ===== 생성 =====

    public static Game candidateFromSteam(int appid, String title, String slug) {
        return create(GameSource.STEAM, String.valueOf(appid), title, slug);
    }

    public static Game manual(String title, String slug) {
        return create(GameSource.MANUAL, null, title, slug);
    }

    private static Game create(GameSource source, String externalId, String title, String slug) {
        Game game = new Game();
        game.source = source;
        game.externalId = externalId;
        game.title = requireText(title, "title");
        game.slug = requireText(slug, "slug");
        game.status = GameStatus.CANDIDATE;
        return game;
    }

    // ===== Steam 소유 필드 =====

    /**
     * Steam이 소유한 필드만 갱신한다. status/slug/genres는 큐레이터 소유이므로 건드리지 않는다.
     */
    public void applySteamData(SteamGameData data) {
        Objects.requireNonNull(data, "data");
        if (source != GameSource.STEAM) {
            throw new DomainStateException("Steam data can only be applied to STEAM games: source=" + source);
        }
        this.title = data.title();
        this.shortDescription = data.shortDescription();
        this.headerImageUrl = data.headerImageUrl();
        this.releaseDate = data.releaseDate();
        this.releaseDateText = data.releaseDateText();
        this.comingSoon = data.comingSoon();
        replaceMedia(data.media());
        replaceDevelopers(data.developers());
        upsertStoreLink(Store.STEAM, STEAM_STORE_URL + externalId);
    }

    private void replaceMedia(List<SteamGameData.Media> items) {
        media.clear();
        int order = 0;
        for (SteamGameData.Media item : items) {
            media.add(GameMedia.of(this, item.type(), item.url(), item.thumbnailUrl(), order++));
        }
    }

    /**
     * 결과는 전체 교체와 같지만, 이미 있는 (developer, role)은 그대로 둔다.
     * 같은 PK를 삭제 후 재삽입하면 Hibernate flush 순서(insert가 delete보다 먼저)로 PK 충돌이 난다.
     */
    private void replaceDevelopers(List<SteamGameData.Credit> credits) {
        developers.removeIf(existing -> credits.stream()
                .noneMatch(c -> existing.matches(c.developer(), c.role())));
        for (SteamGameData.Credit credit : credits) {
            boolean present = developers.stream()
                    .anyMatch(existing -> existing.matches(credit.developer(), credit.role()));
            if (!present) {
                developers.add(GameDeveloper.of(this, credit.developer(), credit.role()));
            }
        }
    }

    private void upsertStoreLink(Store store, String url) {
        storeLinks.stream()
                .filter(link -> link.getStore() == store)
                .findFirst()
                .ifPresentOrElse(
                        link -> link.changeUrl(url),
                        () -> storeLinks.add(StoreLink.of(this, store, url)));
    }

    // ===== 큐레이터 소유 필드 =====

    public void changeSlug(String slug) {
        this.slug = requireText(slug, "slug");
    }

    public void replaceGenres(Set<Genre> genres) {
        Objects.requireNonNull(genres, "genres");
        this.genres.clear();
        this.genres.addAll(genres);
    }

    public void hide() {
        this.status = GameStatus.HIDDEN;
    }

    /**
     * 숨김을 풀면 CANDIDATE로 돌아간다. 다시 노출하려면 publish를 호출해야 한다.
     */
    public void unhide() {
        if (status != GameStatus.HIDDEN) {
            throw new DomainStateException("Only HIDDEN games can be unhidden: status=" + status);
        }
        this.status = GameStatus.CANDIDATE;
    }

    // ===== 공개 =====

    public void publish(boolean hasPublishedArticle, Instant now) {
        Objects.requireNonNull(now, "now");
        if (!hasPublishedArticle) {
            throw new DomainStateException("A published curation article is required to publish a game");
        }
        this.status = GameStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public boolean isPublished() {
        return status == GameStatus.PUBLISHED;
    }

    // ===== 컬렉션 조회 (읽기 전용) =====

    public List<GameMedia> getMedia() {
        return Collections.unmodifiableList(media);
    }

    public List<StoreLink> getStoreLinks() {
        return Collections.unmodifiableList(storeLinks);
    }

    public Set<Genre> getGenres() {
        return Collections.unmodifiableSet(genres);
    }

    public List<GameDeveloper> getDevelopers() {
        return Collections.unmodifiableList(developers);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Game." + field + " must not be blank");
        }
        return value;
    }
}
