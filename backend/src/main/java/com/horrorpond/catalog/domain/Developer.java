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
@Table(name = "developer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Developer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String websiteUrl;

    public static Developer create(String name, String slug) {
        if (name == null || name.isBlank() || slug == null || slug.isBlank()) {
            throw new DomainValidationException("Developer name/slug must not be blank");
        }
        Developer developer = new Developer();
        developer.name = name;
        developer.slug = slug;
        return developer;
    }

    public void changeWebsite(String url) {
        this.websiteUrl = url;
    }
}
