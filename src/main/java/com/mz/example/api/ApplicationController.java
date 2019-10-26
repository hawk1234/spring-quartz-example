package com.mz.example.api;

import com.mz.example.service.SchedulerService;
import com.mz.example.service.response.JobStatusResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
public class ApplicationController {

    @Autowired
    private SchedulerService schedulerService;

    @GetMapping(ApplicationControllerMapping.SCHEDULE_JOB_METHOD)
    public ResponseEntity<String> handleScheduleJob() throws SchedulerException {
        String jobId = schedulerService.scheduleJob();
        return ResponseEntity
                .created(UriComponentsBuilder
                        .fromPath(ApplicationControllerMapping.JOB_STATUS_METHOD)
                        .buildAndExpand(jobId).toUri())
                .body(jobId);
    }

    @GetMapping(ApplicationControllerMapping.JOB_STATUS_METHOD)
    public ResponseEntity<JobStatusResponse> handleJobStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(schedulerService.getJobStatus(jobId));
    }

    @UtilityClass
    static class ApplicationControllerMapping {

        private static final String BASE_PATH = "/api";

        static final String SCHEDULE_JOB_METHOD = BASE_PATH+"/schedule";
        static final String JOB_STATUS_METHOD = BASE_PATH+"/{jobId}/status";
    }
}
