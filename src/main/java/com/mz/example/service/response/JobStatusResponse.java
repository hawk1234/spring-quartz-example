package com.mz.example.service.response;

import com.mz.example.quartz.history.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

import java.time.ZonedDateTime;

@AllArgsConstructor
public final class JobStatusResponse {

    @Delegate(types = JobStatusResponseInterface.class)
    private final JobStatus jobStatus;

    private interface JobStatusResponseInterface {
        String getJobId();
        String getJobName();
        ZonedDateTime getJobCreated();
        ZonedDateTime getJobStarted();
        ZonedDateTime getJobFinished();
        boolean isFinishedSuccessfully();
    }
}
