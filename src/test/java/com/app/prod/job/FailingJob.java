package com.app.prod.job;

import com.app.prod.job.jobs.Job;

/** A job whose downstream is down: it fails every time it runs. Test-only, registered by the test that uses it. */
record FailingJob(String note) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        throw new IllegalStateException("downstream is down: " + note);
    }
}
