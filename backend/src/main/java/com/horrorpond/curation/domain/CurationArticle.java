package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.domain.Language;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleKind kind = ArticleKind.PICK;

    private Instant publishedAt;

    /** 영어 번역 (선택). 제목·한 줄 소개·본문은 함께 있거나 함께 없다 */
    @Column(length = MAX_TITLE_LENGTH)
    private String titleEn;

    @Column(length = MAX_ONE_LINER_LENGTH)
    private String oneLinerEn;

    @Column(columnDefinition = "text")
    private String bodyEn;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> highlightsEn;

    /** 번역할 때의 한국어 글 요약값 (sourceDigest). 지금 값과 다르면 번역이 오래된 것 */
    @Column(length = 64)
    private String enSourceDigest;

    public static CurationArticle draft(Long gameId, String title, String oneLiner, String body,
                                        List<String> highlights) {
        return draft(gameId, ArticleKind.PICK, title, oneLiner, body, highlights);
    }

    public static CurationArticle draft(Long gameId, ArticleKind kind, String title, String oneLiner, String body,
                                        List<String> highlights) {
        Objects.requireNonNull(gameId, "gameId");
        CurationArticle article = new CurationArticle();
        article.gameId = gameId;
        article.status = ArticleStatus.DRAFT;
        article.edit(kind, title, oneLiner, body, highlights);
        return article;
    }

    public void edit(String title, String oneLiner, String body, List<String> highlights) {
        edit(kind, title, oneLiner, body, highlights);
    }

    /**
     * 종류도 함께 바꾼다. 공개된 추천 글은 장점 포인트가 1개 이상이어야 한다 (후기는 없어도 된다).
     */
    public void edit(ArticleKind kind, String title, String oneLiner, String body, List<String> highlights) {
        Objects.requireNonNull(kind, "kind");
        List<String> validated = validateHighlights(highlights);
        if (isPublished() && kind == ArticleKind.PICK && validated.isEmpty()) {
            throw new DomainValidationException("A published article needs at least one highlight");
        }
        this.kind = kind;
        this.title = requireText(title, "title", MAX_TITLE_LENGTH);
        this.oneLiner = requireText(oneLiner, "oneLiner", MAX_ONE_LINER_LENGTH);
        this.body = requireText(body, "body", Integer.MAX_VALUE);
        this.highlights = validated;
    }

    /**
     * 영어 번역을 넣거나 바꾼다. 지금의 한국어 글을 기준으로 번역했다고 기록한다.
     * 장점 포인트는 한국어와 같은 규칙(최대 5개, 40자)이며 비워 둘 수 있다.
     */
    public void translate(String titleEn, String oneLinerEn, String bodyEn, List<String> highlightsEn) {
        String title = requireText(titleEn, "titleEn", MAX_TITLE_LENGTH);
        String oneLiner = requireText(oneLinerEn, "oneLinerEn", MAX_ONE_LINER_LENGTH);
        String body = requireText(bodyEn, "bodyEn", Integer.MAX_VALUE);
        List<String> highlights = validateHighlights(highlightsEn);
        this.titleEn = title;
        this.oneLinerEn = oneLiner;
        this.bodyEn = body;
        this.highlightsEn = highlights;
        this.enSourceDigest = sourceDigest();
    }

    public void clearTranslation() {
        this.titleEn = null;
        this.oneLinerEn = null;
        this.bodyEn = null;
        this.highlightsEn = null;
        this.enSourceDigest = null;
    }

    public boolean hasTranslation() {
        return titleEn != null;
    }

    /** 번역한 뒤 한국어 글(제목·한 줄 소개·본문·장점)이 바뀌었는지 */
    public boolean isTranslationStale() {
        return hasTranslation() && !sourceDigest().equals(enSourceDigest);
    }

    /** 영어 화면이고 번역이 있으면 영어, 아니면 한국어 */
    public boolean isShownIn(Language language) {
        return language == Language.EN && hasTranslation();
    }

    public String title(Language language) {
        return isShownIn(language) ? titleEn : title;
    }

    public String oneLiner(Language language) {
        return isShownIn(language) ? oneLinerEn : oneLiner;
    }

    public String body(Language language) {
        return isShownIn(language) ? bodyEn : body;
    }

    public List<String> highlights(Language language) {
        return isShownIn(language) ? getHighlightsEn() : getHighlights();
    }

    public List<String> getHighlightsEn() {
        return highlightsEn == null ? List.of() : Collections.unmodifiableList(highlightsEn);
    }

    /** 목록 조회(QueryDSL 행)에서도 같은 규칙으로 고른다 */
    public static String pick(Language language, String korean, String english) {
        return language == Language.EN && english != null ? english : korean;
    }

    public static List<String> pickHighlights(Language language, String englishOneLiner, List<String> korean,
                                              List<String> english) {
        if (language == Language.EN && englishOneLiner != null) {
            return english == null ? List.of() : english;
        }
        return korean == null ? List.of() : korean;
    }

    private String sourceDigest() {
        String source = String.join("\u0000", title, oneLiner, body, String.join("\u0001", highlights));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
        if (!isPublishable()) {
            throw new DomainValidationException("At least one highlight is required to publish");
        }
        this.status = ArticleStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    /**
     * 추천을 내린다. 글은 초안으로 남아 다시 공개할 수 있다 (최초 공개 시점은 유지).
     */
    public void unpublish() {
        this.status = ArticleStatus.DRAFT;
    }

    /** 추천은 장점 포인트가 1개 이상일 때, 후기는 언제나 공개할 수 있다 */
    public boolean isPublishable() {
        return kind == ArticleKind.REVIEW || !highlights.isEmpty();
    }

    public boolean isPick() {
        return kind == ArticleKind.PICK;
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
