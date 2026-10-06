package com.horrorpond.catalog.repository;

import com.horrorpond.catalog.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findBySlug(String slug);

    List<Genre> findAllByOrderByDisplayOrderAsc();

    List<Genre> findBySlugIn(Collection<String> slugs);
}
