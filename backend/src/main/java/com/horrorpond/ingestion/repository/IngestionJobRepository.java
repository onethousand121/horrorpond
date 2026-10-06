package com.horrorpond.ingestion.repository;

import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {

    List<IngestionJob> findByStatusAndStartedAtBefore(JobStatus status, Instant startedBefore);

    List<IngestionJob> findAllByOrderByIdDesc(Limit limit);
}
