package com.mz.example.quartz.history.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Getter
@Entity
@Table(name = "JOB_STATUS")
@NoArgsConstructor
public class JobStatus {

    @Id
    @Column(name = "JOB_ID")
    private String jobId;
    @NonNull
    @Column(name = "JOB_NAME")
    private String jobName;
    @Column(name = "JOB_CREATED")
    private ZonedDateTime jobCreated;
    @Column(name = "JOB_STARTED")
    private ZonedDateTime jobStarted;
    @Column(name = "JOB_FINISHED")
    private ZonedDateTime jobFinished;
    @NotNull
    @Column(name = "FINISHED_SUCCESSFULLY")
    private boolean finishedSuccessfully = false;

    public JobStatus(String jobId, String jobName){
        this.jobId = jobId;
        this.jobName = jobName;
    }

    public JobStatus jobCreated(){
        jobCreated = ZonedDateTime.now();
        return this;
    }

    public JobStatus jobStarted(){
        jobStarted = ZonedDateTime.now();
        return this;
    }

    public JobStatus jobFinished(){
        jobFinished = ZonedDateTime.now();
        return this;
    }

    public JobStatus finishedSuccessfully() {
        this.finishedSuccessfully = true;
        return this;
    }
}
