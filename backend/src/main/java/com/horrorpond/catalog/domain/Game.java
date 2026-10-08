package com.horrorpond.catalog.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.Language;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** 영어 Steam 데이터 (l=english). 없으면 한국어 값을 쓴다 */
    @Column(length = 300)
    private String titleEn;

    @Column(columnDefinition = "text")
    private String shortDescriptionEn;

    @Column(length = 100)
    private String releaseDateTextEn;

    @Column(nullable = false)
    private boolean comingSoon;

    /**
     * STEAM 게임은 Steam categories에서 파생된다. MANUAL/ITCH 게임은 큐레이터가 정한다.
     */
    @Column(nullable = false)
    private boolean coop;

    /** Steam 리뷰 수. 출시작 자동 노출의 품질 하한에 쓴다 */
    private Integer reviewCount;

    /** 성인 콘텐츠. 자동 노출에서 제외되고, 큐레이터가 공개(PUBLISHED)해야 보인다 */
    @Column(nullable = false)
    private boolean adult;

    /** SteamSpy 상위 태그(표가 많은 순). 장르 자동 분류에 쓴다 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "varchar(100)[]")
    private List<String> tags = new ArrayList<>();

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
     * Steam이 소유한 필드(coop 포함)만 갱신한다. status/slug/genres는 큐레이터 소유이므로 건드리지 않는다.
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
        this.coop = data.coop();
        this.reviewCount = data.reviewCount();
        this.adult = data.adult();
        replaceMedia(data.media());
        replaceDevelopers(data.developers());
        upsertStoreLink(Store.STEAM, STEAM_STORE_URL + externalId);
    }

    /**
     * 리뷰 수만 새로 받았을 때 (하루 1회 리뷰 수 기록). 다른 Steam 필드는 다음 상세 갱신 때 바뀐다.
     */
    public void updateReviewCount(int reviewCount) {
        if (source != GameSource.STEAM) {
            throw new DomainStateException("Steam review count can only be applied to STEAM games: source=" + source);
        }
        if (reviewCount < 0) {
            throw new DomainValidationException("reviewCount must not be negative: " + reviewCount);
        }
        this.reviewCount = reviewCount;
    }

    /**
     * 영어 Steam 데이터. 수집 시 영어 응답을 못 받았으면 호출하지 않아 이전 값을 유지한다.
     */
    public void applyEnglishText(String title, String shortDescription, String releaseDateText) {
        this.titleEn = title;
        this.shortDescriptionEn = shortDescription;
        this.releaseDateTextEn = releaseDateText;
    }

    public String title(Language language) {
        return language.pick(title, titleEn);
    }

    public String shortDescription(Language language) {
        return language.pick(shortDescription, shortDescriptionEn);
    }

    public String releaseDateText(Language language) {
        return language.pick(releaseDateText, releaseDateTextEn);
    }

    public void applySteamTags(List<String> tags) {
        this.tags = new ArrayList<>(tags == null ? List.of() : tags);
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

    /**
     * STEAM 게임의 coop은 Steam 데이터가 소유하므로(다음 normalize에서 덮어씀) 큐레이터 변경을 막는다.
     */
    public void changeCoop(boolean coop) {
        if (source == GameSource.STEAM) {
            throw new DomainStateException("coop of STEAM games is derived from Steam categories");
        }
        this.coop = coop;
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

    /**
     * 큐레이터가 직접 공개한다. 자동 노출 조건(리뷰 수, 성인 여부)과 무관하게 항상 노출된다.
     */
    public void publish(Instant now) {
        Objects.requireNonNull(now, "now");
        this.status = GameStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public boolean isPublished() {
        return status == GameStatus.PUBLISHED;
    }

    /**
     * 공개 사이트 노출 규칙 (CuratedGameQueryRepository의 목록 조건과 같아야 한다).
     * - HIDDEN: 노출 안 함
     * - PUBLISHED: 큐레이터가 공개한 게임. 항상 노출
     * - CANDIDATE: 수집된 공포게임. {@link AutoExposure} 기준을 넘으면 자동 노출
     */
    public boolean isPubliclyVisible(AutoExposure rule, LocalDate today) {
        return isPubliclyVisible(status, adult, comingSoon, reviewCount, releaseDate, rule, today);
    }

    /** 엔티티 없이 조회 결과(projection)로 판정할 때 쓴다 */
    public static boolean isPubliclyVisible(GameStatus status, boolean adult, boolean comingSoon,
                                            Integer reviewCount, LocalDate releaseDate, AutoExposure rule,
                                            LocalDate today) {
        return switch (status) {
            case HIDDEN -> false;
            case PUBLISHED -> true;
            case CANDIDATE -> rule.allows(adult, comingSoon, reviewCount, releaseDate, today);
        };
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
