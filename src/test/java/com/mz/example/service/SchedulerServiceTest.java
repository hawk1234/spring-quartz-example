package com.mz.example.service;

import com.mz.example.quartz.ClearingHistoryJob;
import com.mz.example.quartz.history.entity.JobStatus;
import com.mz.example.quartz.history.repository.JobStatusRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class SchedulerServiceTest extends SchedulerServiceBaseTest {

    @Autowired
    private JobStatusRepository jobStatusRepository;
    @MockBean
    private ClearingHistoryJob.Config clearingHistoryConfig;

    @Before
    public void setup() throws SchedulerException {
        super.setup();
        jobStatusRepository.deleteAll();
        Assert.assertEquals(0, jobStatusRepository.count());
    }

    @Test
    public void testClearingHistory() throws Exception{
        jobStatusRepository.save(new JobStatus("job1", "TEST_JOB").jobCreated());
        Assert.assertEquals(1, jobStatusRepository.count());
        Mockito.when(clearingHistoryConfig.getDaysToExpiry()).thenReturn(-1);
        fireClearingHistoryJob();
        Assert.assertEquals(0, jobStatusRepository.count());
    }

    @Test
    public void testClearingHistoryKeepsTodayJobsWhenExpiryDaysIsSetToZero() throws Exception{
        jobStatusRepository.save(new JobStatus("job1", "TEST_JOB").jobCreated());
        Assert.assertEquals(1, jobStatusRepository.count());
        Mockito.when(clearingHistoryConfig.getDaysToExpiry()).thenReturn(0);
        fireClearingHistoryJob();
        Assert.assertEquals(1, jobStatusRepository.count());
    }
}
