package com.horrorpond.ingestion.domain;

import com.horrorpond.common.domain.DomainStateException;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HorrorTag horrorTag;

    private Instant horrorTagCheckedAt;

    /** SteamSpy 상위 태그(표가 많은 순). 아직 받지 않았으면 null */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(100)[]")
    private List<String> spyTags;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static SteamAppSeed discovered(int appid, DiscoveredBy by, Instant now) {
        SteamAppSeed seed = new SteamAppSeed();
        seed.appid = appid;
        seed.discoveredBy = Objects.requireNonNull(by, "by");
        seed.discoveredAt = Objects.requireNonNull(now, "now");
        seed.fetchStatus = FetchStatus.PENDING;
        seed.horrorTag = HorrorTag.UNCHECKED;
        return seed;
    }

    /**
     * 자동으로 발견했고(SteamSpy 태그, Steam 검색) 아직 판정 전인 seed만 판정한다. 수동 추가는 관리자가 원한 것이라 판정하지 않는다.
     */
    public boolean needsHorrorTagCheck() {
        return discoveredBy != DiscoveredBy.MANUAL && horrorTag == HorrorTag.UNCHECKED;
    }

    /**
     * 태그는 공포 판정과 별개로 모든 seed가 한 번 받는다(장르 자동 분류용). 수동 추가 seed도 포함.
     */
    public boolean needsSpyTags() {
        return spyTags == null;
    }

    public void recordSpyTags(List<String> tags) {
        this.spyTags = new ArrayList<>(Objects.requireNonNull(tags, "tags"));
    }

    public void recordHorrorTag(HorrorTag result, Instant now) {
        if (result == HorrorTag.UNCHECKED) {
            throw new DomainValidationException("recordHorrorTag accepts HORROR or NOT_HORROR only");
        }
        this.horrorTag = result;
        this.horrorTagCheckedAt = Objects.requireNonNull(now, "now");
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

    /**
     * 관리자가 이미 발견된(아직 수집 안 된) 게임을 직접 요청했다: 수동 추가처럼 가장 먼저, 공포 판정 없이 수집한다.
     * (자동 발견에서 공포가 아니라고 걸러졌거나 실패가 쌓인 seed도 다시 받는다)
     */
    public void requestManually() {
        if (fetchStatus == FetchStatus.OK) {
            throw new DomainStateException("Already collected: appid=" + appid);
        }
        this.discoveredBy = DiscoveredBy.MANUAL;
        this.fetchStatus = FetchStatus.PENDING;
        this.failCount = 0;
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
