package com.horrorpond.catalog.repository;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.GameSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySourceAndExternalId(GameSource source, String externalId);

    boolean existsBySlug(String slug);

    Optional<Game> findBySlug(String slug);
}
