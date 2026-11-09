package com.horrorpond.curation.repository;

import com.horrorpond.curation.domain.DailyPickCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPickCandidateRepository extends JpaRepository<DailyPickCandidate, Long> {

    List<DailyPickCandidate> findAllByOrderByIdAsc();

    Optional<DailyPickCandidate> findByGameId(Long gameId);

    Optional<DailyPickCandidate> findByFeaturedOn(LocalDate featuredOn);
}
