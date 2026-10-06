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
