package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.CurationArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurationArticleRepository extends JpaRepository<CurationArticle, Long> {

    Optional<CurationArticle> findByGameId(Long gameId);
}
