package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
}
