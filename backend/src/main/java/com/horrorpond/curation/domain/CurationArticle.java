package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "curation_article")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class CurationArticle extends BaseTimeEntity {

    public static final int MAX_HIGHLIGHTS = 5;
    public static final int MAX_HIGHLIGHT_LENGTH = 40;
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_ONE_LINER_LENGTH = 120;
    public static final int MAX_DISCLOSURE_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long gameId;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, length = MAX_ONE_LINER_LENGTH)
    private String oneLiner;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> highlights = new ArrayList<>();

    @Column(nullable = false)
    private boolean sponsored;

    @Column(length = MAX_DISCLOSURE_LENGTH)
    private String sponsorDisclosure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status;

    private Instant publishedAt;

    public static CurationArticle draft(Long gameId, String title, String oneLiner, String body,
                                        List<String> highlights) {
        Objects.requireNonNull(gameId, "gameId");
        CurationArticle article = new CurationArticle();
        article.gameId = gameId;
        article.status = ArticleStatus.DRAFT;
        article.edit(title, oneLiner, body, highlights);
        return article;
    }

    public void edit(String title, String oneLiner, String body, List<String> highlights) {
        List<String> validated = validateHighlights(highlights);
        if (isPublished() && validated.isEmpty()) {
            throw new DomainValidationException("A published article needs at least one highlight");
        }
        this.title = requireText(title, "title", MAX_TITLE_LENGTH);
        this.oneLiner = requireText(oneLiner, "oneLiner", MAX_ONE_LINER_LENGTH);
        this.body = requireText(body, "body", Integer.MAX_VALUE);
        this.highlights = validated;
    }

    public void markSponsored(String disclosure) {
        this.sponsorDisclosure = requireText(disclosure, "sponsorDisclosure", MAX_DISCLOSURE_LENGTH);
        this.sponsored = true;
    }

    public void clearSponsored() {
        this.sponsored = false;
        this.sponsorDisclosure = null;
    }

    /**
     * publishedAt은 최초 공개 시점만 기록한다.
     */
    public void publish(Instant now) {
        Objects.requireNonNull(now, "now");
        if (highlights.isEmpty()) {
            throw new DomainValidationException("At least one highlight is required to publish");
        }
        this.status = ArticleStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }

    public List<String> getHighlights() {
        return Collections.unmodifiableList(highlights);
    }

    private static List<String> validateHighlights(List<String> highlights) {
        if (highlights == null) {
            return new ArrayList<>();
        }
        if (highlights.size() > MAX_HIGHLIGHTS) {
            throw new DomainValidationException(
                    "At most " + MAX_HIGHLIGHTS + " highlights are allowed: " + highlights.size());
        }
        List<String> result = new ArrayList<>(highlights.size());
        for (String item : highlights) {
            if (item == null || item.isBlank()) {
                throw new DomainValidationException("Highlight must not be blank");
            }
            String stripped = item.strip();
            if (stripped.length() > MAX_HIGHLIGHT_LENGTH) {
                throw new DomainValidationException(
                        "Highlight must be at most " + MAX_HIGHLIGHT_LENGTH + " characters: " + stripped);
            }
            result.add(stripped);
        }
        return result;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("CurationArticle." + field + " must not be blank");
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new DomainValidationException(
                    "CurationArticle." + field + " must be at most " + maxLength + " characters");
        }
        return stripped;
    }
}
