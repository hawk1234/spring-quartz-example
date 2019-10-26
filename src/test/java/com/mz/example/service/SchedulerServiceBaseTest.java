package com.mz.example.service;

import com.mz.example.AbstractApplicationTest;
import com.mz.example.TestUtil;
import org.junit.Before;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

public abstract class SchedulerServiceBaseTest extends AbstractApplicationTest {

    private final static Object SYNC = new Object();

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private SchedulerService schedulerService;
    private HashMap<String, Boolean> jobIdToFinished = new HashMap<>();

    @Before
    public void setup() throws SchedulerException {
        synchronized (SYNC) {
            jobIdToFinished.clear();
        }
        scheduler.getListenerManager().addJobListener(new JobListener());
    }

    protected void fireClearingHistoryJob() throws InterruptedException, SchedulerException{
        final String jobId = fireClearingHistoryJobInternal();
        TestUtil.conditionalWait(() -> !jobIdToFinished.get(jobId));
    }

    private String fireClearingHistoryJobInternal() throws SchedulerException {
        synchronized (SYNC) {
            String jobId = schedulerService.clearJobHistory();
            jobIdToFinished.put(jobId, false);
            return jobId;
        }
    }

    private class JobListener extends JobListenerSupport {

        @Override
        public String getName() {
            return "TEST_LISTENER";
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            final String jobId = context.getTrigger().getKey().getName();
            synchronized (SYNC) {
                //NOTE: this works cause quartz scheduler uses LinkedHashMap (order preserved) for listeners,
                // otherwise it wouldn't be certain that this listener will be called after
                // SchedulerService listener.
                jobIdToFinished.put(jobId, true);
            }
        }
    }
}
