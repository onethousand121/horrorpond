package com.horrorpond.ingestion.domain;

import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.Objects;

/**
 * appdetails 원본 응답. appid를 직접 할당하므로 {@link Persistable} 패턴을 쓴다.
 */
@Getter
@Entity
@Table(name = "steam_raw_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "payload")
public class SteamRawSnapshot implements Persistable<Integer> {

    private static final int HASH_LENGTH = 64;

    @Id
    private Integer appid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = HASH_LENGTH)
    private String payloadHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = HASH_LENGTH)
    private String normalizedHash;

    @Column(nullable = false)
    private Instant fetchedAt;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static SteamRawSnapshot of(int appid, String payload, String hash, Instant now) {
        SteamRawSnapshot snapshot = new SteamRawSnapshot();
        snapshot.appid = appid;
        snapshot.replace(payload, hash, now);
        return snapshot;
    }

    public void replace(String payload, String hash, Instant now) {
        if (payload == null || payload.isBlank()) {
            throw new DomainValidationException("payload must not be blank");
        }
        if (hash == null || hash.length() != HASH_LENGTH) {
            throw new DomainValidationException("hash must be " + HASH_LENGTH + " characters");
        }
        this.payload = payload;
        this.payloadHash = hash;
        this.fetchedAt = Objects.requireNonNull(now, "now");
    }

    public void markNormalized() {
        this.normalizedHash = payloadHash;
    }

    public boolean needsNormalize() {
        return !payloadHash.equals(normalizedHash);
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
