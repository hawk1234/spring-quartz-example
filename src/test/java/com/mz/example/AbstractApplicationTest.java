package com.mz.example;

import com.mz.example.service.SchedulerService;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Objects;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:test.properties")
public abstract class AbstractApplicationTest {

    @Autowired
    private SchedulerService schedulerService;

    @LocalServerPort
    private int serverPort;

    protected int getServerPort(){
        return serverPort;
    }

    protected String scheduleJob() throws SchedulerException {
        return schedulerService.scheduleJob();
    }

    protected String waitJobStarted(String jobId) throws InterruptedException{
        TestUtil.conditionalWait(() -> Objects.isNull(schedulerService.getJobStatus(jobId).getJobStarted()));
        return jobId;
    }

    protected String waitJobFinished(String jobId) throws InterruptedException{
        TestUtil.conditionalWait(() -> Objects.isNull(schedulerService.getJobStatus(jobId).getJobFinished()));
        return jobId;
    }
}
