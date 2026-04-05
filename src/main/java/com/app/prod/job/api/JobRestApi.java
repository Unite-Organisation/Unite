package com.app.prod.job.api;

import com.app.prod.job.dtos.JobResponse;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.service.JobErrorService;
import com.app.prod.job.service.JobRerunService;
import com.app.prod.schedulers.FailedJobsScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("job")
@RequiredArgsConstructor
public class JobRestApi {

    private final JobErrorService jobErrorService;
    private final JobRerunService jobRerunService;
    private final FailedJobsScheduler failedJobsScheduler;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public List<JobResponse> getJobs(@RequestParam(required = false) JobStatus status) {
        return jobErrorService.getJobs(status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/rerun")
    public ResponseEntity<JobStatus> rerunJob(@PathVariable UUID id) {
        JobStatus jobStatus = jobRerunService.rerunJob(id);
        return ResponseEntity.ok(jobStatus);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rerun-all")
    public void rerunAllFailedJobs() {
        failedJobsScheduler.rerunFailedJobs();
    }
}
