package com.horrorpond.catalog.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "genre")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Genre extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int displayOrder;

    /** 이 장르로 자동 분류할 SteamSpy 태그 (큐레이터가 장르를 직접 붙이지 않은 게임용) */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "varchar(100)[]")
    private List<String> steamTags = new ArrayList<>();

    /** 게임의 SteamSpy 태그 중 하나라도 이 장르의 태그와 겹치면 이 장르로 본다 */
    public boolean matchesAnyTag(List<String> gameTags) {
        return gameTags.stream().anyMatch(steamTags::contains);
    }

    public static Genre create(String name, String slug, String description, int displayOrder) {
        Genre genre = new Genre();
        genre.update(name, slug, description, displayOrder);
        return genre;
    }

    public void update(String name, String slug, String description, int displayOrder) {
        this.name = requireText(name, "name");
        this.slug = requireText(slug, "slug");
        this.description = description;
        this.displayOrder = displayOrder;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Genre." + field + " must not be blank");
        }
        return value;
    }
}
