package com.app.prod.job.api;

import com.app.prod.job.dtos.JobResponse;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.service.JobErrorService;
import com.app.prod.job.service.JobRerunService;
import com.app.prod.services.schedulers.FailedJobsScheduler;
import lombok.RequiredArgsConstructor;
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

    @GetMapping()
    public List<JobResponse> getJobs(@RequestParam(required = false) JobStatus status) {
        return jobErrorService.getJobs(status);
    }

    @PostMapping("/{id}/rerun")
    public void rerunJob(@PathVariable UUID id) {
        jobRerunService.rerunJob(id);
    }

    @PostMapping("/rerun-all")
    public void rerunAllFailedJobs() {
        failedJobsScheduler.rerunFailedJobs();
    }
}
