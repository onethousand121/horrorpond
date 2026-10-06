package com.horrorpond.ingestion.api;

import com.horrorpond.ingestion.application.IngestionTriggerService;
import com.horrorpond.ingestion.application.SeedService;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * /api/admin/** 는 AdminKeyFilter가 X-Admin-Key를 검사한다.
 */
@RestController
@RequestMapping("/api/admin/ingestion")
@RequiredArgsConstructor
public class IngestionAdminController {

    private static final int MAX_JOBS = 100;

    private final IngestionTriggerService triggerService;
    private final SeedService seedService;
    private final IngestionJobRepository jobRepository;

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResponse run(@Valid @RequestBody RunRequest request) {
        triggerService.trigger(request.steps());
        return new RunResponse(request.steps());
    }

    @GetMapping("/jobs")
    public List<JobResponse> jobs(@RequestParam(defaultValue = "20") int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_JOBS));
        return jobRepository.findAllByOrderByIdDesc(Limit.of(bounded)).stream()
                .map(JobResponse::from)
                .toList();
    }

    @PostMapping("/seeds")
    @ResponseStatus(HttpStatus.CREATED)
    public SeedResponse addSeed(@Valid @RequestBody SeedRequest request) {
        return SeedResponse.from(seedService.addManual(request.appid()));
    }

    public record RunRequest(@NotEmpty List<@NotNull JobType> steps) {
    }

    public record RunResponse(List<JobType> steps) {
    }

    public record SeedRequest(@NotNull @Positive Integer appid) {
    }

    public record SeedResponse(int appid, DiscoveredBy discoveredBy, FetchStatus fetchStatus) {

        static SeedResponse from(SteamAppSeed seed) {
            return new SeedResponse(seed.getAppid(), seed.getDiscoveredBy(), seed.getFetchStatus());
        }
    }

    public record JobResponse(Long id, JobType type, TriggerType triggerType, JobStatus status,
                              Instant startedAt, Instant finishedAt, int processedCount, int failedCount,
                              String errorMessage) {

        static JobResponse from(IngestionJob job) {
            return new JobResponse(job.getId(), job.getType(), job.getTriggerType(), job.getStatus(),
                    job.getStartedAt(), job.getFinishedAt(), job.getProcessedCount(), job.getFailedCount(),
                    job.getErrorMessage());
        }
    }
}
