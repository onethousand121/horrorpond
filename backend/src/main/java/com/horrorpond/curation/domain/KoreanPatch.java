package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

/**
 * 유저가 만든 비공식 한국어 패치. 파일은 두지 않고 제작자 배포처 주소(https)만 링크한다.
 */
@Getter
@Entity
@Table(name = "korean_patch")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class KoreanPatch extends BaseTimeEntity {

    public static final int MAX_PER_GAME = 10;
    public static final int MAX_URL_LENGTH = 500;
    public static final int MAX_AUTHOR_LENGTH = 100;
    public static final int MAX_NOTE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = MAX_URL_LENGTH)
    private String url;

    /** 제작자 이름. 없으면 null (사이트는 "유저 한글패치") */
    @Column(length = MAX_AUTHOR_LENGTH)
    private String author;

    /** 짧은 메모 (지원 버전 등). 없으면 null */
    @Column(length = MAX_NOTE_LENGTH)
    private String note;

    @Column(nullable = false)
    private int sortOrder;

    public static KoreanPatch create(Long gameId, String url, String author, String note, int sortOrder) {
        KoreanPatch patch = new KoreanPatch();
        patch.gameId = Objects.requireNonNull(gameId, "gameId");
        patch.url = httpsUrl(url);
        patch.author = PlayVideo.optionalText(author, MAX_AUTHOR_LENGTH, "KoreanPatch.author");
        patch.note = PlayVideo.optionalText(note, MAX_NOTE_LENGTH, "KoreanPatch.note");
        patch.sortOrder = sortOrder;
        return patch;
    }

    /** 호스트가 있는 절대 https 주소만 받는다 (javascript:, http:, 상대 주소 거부) */
    static String httpsUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("KoreanPatch.url must not be blank");
        }
        String url = value.strip();
        if (url.length() > MAX_URL_LENGTH) {
            throw new DomainValidationException("KoreanPatch.url must be at most " + MAX_URL_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new DomainValidationException("KoreanPatch.url is not a valid URL: " + url);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") || uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
            throw new DomainValidationException("KoreanPatch.url must be an absolute https URL: " + url);
        }
        return url;
    }
}
