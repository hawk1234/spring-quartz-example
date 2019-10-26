package com.mz.example.service;

import com.mz.example.quartz.history.entity.JobStatus;
import com.mz.example.quartz.history.repository.JobStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class HistoryUpdateService {

    @Autowired
    private JobStatusRepository jobStatusRepository;

    @Transactional
    public void jobExecutionStarted(String jobId) {
        try {
            Optional<JobStatus> jobStatus = jobStatusRepository.findById(jobId);
            jobStatusRepository.save(jobStatus.get().jobStarted());
        } catch (RuntimeException ex) {
            log.warn("System wasn't able to update started status for job: "+jobId, ex);
        }
    }

    @Transactional
    public void jobExecutionFinished(String jobId, boolean success) {
        try {
            Optional<JobStatus> jobStatus = jobStatusRepository.findById(jobId);
            if (success) {
                jobStatusRepository.save(jobStatus.get().jobFinished().finishedSuccessfully());
            } else {
                jobStatusRepository.save(jobStatus.get().jobFinished());
            }
        } catch (RuntimeException ex) {
            log.warn("System wasn't able to update finished status for job: "+jobId, ex);
        }
    }
}
