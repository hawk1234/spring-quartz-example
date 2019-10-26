package com.mz.example.service;

import com.mz.example.quartz.ClearingHistoryJob;
import com.mz.example.quartz.MyJob;
import com.mz.example.quartz.history.entity.JobStatus;
import com.mz.example.quartz.history.repository.JobStatusRepository;
import com.mz.example.service.exception.JobNotFoundException;
import com.mz.example.service.response.JobStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.NameMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SchedulerService implements JobListener {

    private static final String JOB_NAME = "MY_JOB";
    private static final String SCHEDULER_SERVICE_GROUP = "SCHEDULER_SERVICE";
    private static final String JOB_CLEARING_HISTORY_JOB_NAME = "CLEARING_JOB";

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private HistoryUpdateService historyUpdateService;
    @Autowired
    private JobStatusRepository jobStatusRepository;

    @PostConstruct
    private void init() throws SchedulerException{
        scheduler.getListenerManager().addJobListener(this, NameMatcher.jobNameEquals(JOB_NAME));
        JobDetail jobDetail = JobBuilder.newJob(MyJob.class)
                //This will allow other node to pick up the job if executing node fails.
                // It will not cause job to be re-executed when exception occurs
                // ref: https://stackoverflow.com/questions/19267263/quartz-jobdetail-requestrecovery
                // ref: http://www.quartz-scheduler.org/documentation/quartz-2.2.2/configuration/ConfigJDBCJobStoreClustering.html
                .requestRecovery()
                .storeDurably()
                .withIdentity(JOB_NAME, SCHEDULER_SERVICE_GROUP)
                .build();
        tryToAddJob(jobDetail);
        JobDetail clearHistoryJobDetail = JobBuilder.newJob(ClearingHistoryJob.class)
                .storeDurably()
                .withIdentity(JOB_CLEARING_HISTORY_JOB_NAME, SCHEDULER_SERVICE_GROUP)
                .build();
        tryToAddJob(clearHistoryJobDetail);
        Trigger clearingHistoryJobTrigger = TriggerBuilder.newTrigger()
                .forJob(clearHistoryJobDetail.getKey())
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0))
                .withIdentity(JOB_CLEARING_HISTORY_JOB_NAME, SCHEDULER_SERVICE_GROUP)
                .build();
        tryToAddTrigger(clearingHistoryJobTrigger);
    }

    private void tryToAddJob(JobDetail jobDetail) throws SchedulerException{
        try {
            scheduler.addJob(jobDetail, false);
        }catch(ObjectAlreadyExistsException ex){
            log.warn("Job has not bean added to scheduler as " +
                    "it already exists. Job key: "+jobDetail.getKey());
        }
    }

    private void tryToAddTrigger(Trigger trigger) throws SchedulerException {
        try {
            scheduler.scheduleJob(trigger);
        } catch (ObjectAlreadyExistsException ex) {
            log.warn("Trigger has not bean added to scheduler as " +
                    "it already exists. Trigger key: "+trigger.getKey());
        }
    }

    public String scheduleJob() throws SchedulerException {
        Optional<String> alreadyScheduled = checkJobAlreadyScheduledForExecution();
        if(alreadyScheduled.isPresent()){
            log.info("Returning existing jobId scheduled for execution: "+alreadyScheduled.get());
            return alreadyScheduled.get();
        }else {
            String jobId = UUID.randomUUID().toString();
            jobStatusRepository.save(
                    new JobStatus(jobId, JOB_NAME).jobCreated());
            return scheduleJobForNow(JOB_NAME, jobId);
        }
    }

    private Optional<String> checkJobAlreadyScheduledForExecution() throws SchedulerException{
        return scheduler.getTriggersOfJob(JobKey.jobKey(JOB_NAME, SCHEDULER_SERVICE_GROUP))
                .stream().filter(trigger -> Objects.nonNull(trigger.getNextFireTime()))
                .findFirst().map(Trigger::getKey).map(TriggerKey::getName);
    }

    private String scheduleJobForNow(String jobName, String jobId) throws SchedulerException{
        Trigger nowTrigger = TriggerBuilder.newTrigger()
                .forJob(JobKey.jobKey(jobName, SCHEDULER_SERVICE_GROUP))
                .withIdentity(jobId, SCHEDULER_SERVICE_GROUP)
                .startNow()
                .build();
        scheduler.scheduleJob(nowTrigger);
        return nowTrigger.getKey().getName();
    }

    public JobStatusResponse getJobStatus(String jobId) {
        return new JobStatusResponse(
                jobStatusRepository.findById(jobId).orElseThrow(JobNotFoundException::new));
    }

    String clearJobHistory() throws SchedulerException{
        return scheduleJobForNow(JOB_CLEARING_HISTORY_JOB_NAME, UUID.randomUUID().toString());
    }

    //<editor-fold desc="JobListener: Listen for jobs being executed on this node and update job history accordingly.
    // NOTE: This is done in separate transaction and in separate DB, so history might break ACID rules.">
    @Override
    public String getName() {
        return SCHEDULER_SERVICE_GROUP;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        String jobId = findOriginalTriggerKey(context).getName();
        historyUpdateService.jobExecutionStarted(jobId);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        log.warn("Unexpected job vetoed: " + context.getJobDetail().getKey().getName());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobId = findOriginalTriggerKey(context).getName();
        historyUpdateService.jobExecutionFinished(jobId, jobException == null);
    }

    private TriggerKey findOriginalTriggerKey(JobExecutionContext context){
        return context.isRecovering() ?
                context.getRecoveringTriggerKey() : context.getTrigger().getKey();
    }
    //</editor-fold>
}
