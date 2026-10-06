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
 * appid를 직접 할당하므로 {@link Persistable}로 신규 여부를 알려 save 시 merge(select)를 피한다.
 */
@Getter
@Entity
@Table(name = "steam_app_seed")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class SteamAppSeed implements Persistable<Integer> {

    @Id
    private Integer appid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscoveredBy discoveredBy;

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

    public static SteamAppSeed discovered(int appid, DiscoveredBy by, Instant now) {
        SteamAppSeed seed = new SteamAppSeed();
        seed.appid = appid;
        seed.discoveredBy = Objects.requireNonNull(by, "by");
        seed.discoveredAt = Objects.requireNonNull(now, "now");
        seed.fetchStatus = FetchStatus.PENDING;
        return seed;
    }

    /**
     * 조회 결과(OK/NOT_FOUND)를 기록한다. 실패는 {@link #markFailed}를 쓴다.
     */
    public void markFetched(FetchStatus status, Instant now) {
        if (status != FetchStatus.OK && status != FetchStatus.NOT_FOUND) {
            throw new DomainValidationException("markFetched accepts OK or NOT_FOUND only: " + status);
        }
        this.fetchStatus = status;
        this.lastFetchedAt = Objects.requireNonNull(now, "now");
    }

    public void markFailed(Instant now) {
        this.fetchStatus = FetchStatus.FAILED;
        this.lastFetchedAt = Objects.requireNonNull(now, "now");
        this.failCount++;
    }

    @Override
    public Integer getId() {
        return appid;
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
