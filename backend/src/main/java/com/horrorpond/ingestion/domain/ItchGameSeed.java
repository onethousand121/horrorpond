package com.horrorpond.ingestion.domain;

import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;

/**
 * itch.io 게임 1개. itch 게임 id를 직접 할당하므로 {@link Persistable}로 신규 여부를 알려 save 시 merge를 피한다.
 * 게임잼 작품이 너무 많아 공포 태그 전체가 아니라 인기 게임(평가 수 기준)과 관리자가 고른 게임만 들어온다.
 */
@Getter
@Entity
@Table(name = "itch_game_seed")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class ItchGameSeed implements Persistable<Long> {

    @Id
    private Long itchId;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItchDiscoveredBy discoveredBy;

    /** 목록이나 게임 페이지에서 마지막으로 본 평가 수 */
    private Integer ratingCount;

    @Column(nullable = false)
    private Instant discoveredAt;

    private Instant lastFetchedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FetchStatus fetchStatus;

    @Column(nullable = false)
    private int failCount;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static ItchGameSeed discovered(long itchId, String url, ItchDiscoveredBy by, Instant now) {
        ItchGameSeed seed = new ItchGameSeed();
        seed.itchId = itchId;
        seed.url = requireUrl(url);
        seed.discoveredBy = Objects.requireNonNull(by, "by");
        seed.discoveredAt = Objects.requireNonNull(now, "now");
        seed.fetchStatus = FetchStatus.PENDING;
        return seed;
    }

    /** 목록에서 다시 봤다: 주소(개발자가 바꿀 수 있다)와 평가 수를 갱신한다 */
    public void seen(String url, Integer ratingCount) {
        this.url = requireUrl(url);
        if (ratingCount != null) {
            this.ratingCount = ratingCount;
        }
    }

    /**
     * 관리자가 직접 골랐다. 자동 발견으로 들어온 게임이어도 관리자 선택으로 바꿔, 인기 기준과 상관없이 계속 갱신한다.
     */
    public void requestManually(String url) {
        this.url = requireUrl(url);
        this.discoveredBy = ItchDiscoveredBy.MANUAL;
        this.failCount = 0;
    }

    public void markFetched(FetchStatus status, Integer ratingCount, Instant now) {
        if (status != FetchStatus.OK && status != FetchStatus.NOT_FOUND) {
            throw new DomainValidationException("markFetched accepts OK or NOT_FOUND only: " + status);
        }
        this.fetchStatus = status;
        this.lastFetchedAt = Objects.requireNonNull(now, "now");
        this.failCount = 0;
        if (ratingCount != null) {
            this.ratingCount = ratingCount;
        }
    }

    public void markFailed(Instant now) {
        this.fetchStatus = FetchStatus.FAILED;
        this.lastFetchedAt = Objects.requireNonNull(now, "now");
        this.failCount++;
    }

    private static String requireUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new DomainValidationException("ItchGameSeed.url must not be blank");
        }
        return url;
    }

    @Override
    public Long getId() {
        return itchId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
