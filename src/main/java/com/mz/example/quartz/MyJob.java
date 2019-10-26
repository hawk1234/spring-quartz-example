package com.mz.example.quartz;

import com.mz.example.service.MyJobService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public final class MyJob extends QuartzJobBean {

    @Autowired
    private MyJobService jobService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            jobService.doStuff();
        } catch (Throwable ex) {
            throw new JobExecutionException(ex);
        }
    }
}
