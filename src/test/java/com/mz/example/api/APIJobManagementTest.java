package com.mz.example.api;

import com.mz.example.AbstractApplicationTest;
import com.mz.example.service.MyJobService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;

public class APIJobManagementTest extends AbstractApplicationTest {

    @Autowired
    private Scheduler scheduler;
    @MockBean
    private MyJobService myJobService;

    @Test
    public void testJobStatusFailOnJobNotFound() throws Exception {
        RestAssured.with()
                    .port(getServerPort())
                    .pathParam("jobId", "NO_SUCH_JOB")
                .when()
                    .get(ApplicationController.ApplicationControllerMapping.JOB_STATUS_METHOD)
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .body("status", CoreMatchers.equalTo(HttpStatus.NOT_FOUND.value()))
                    .body("message", CoreMatchers.notNullValue())
                    .body("timestamp", CoreMatchers.notNullValue());
    }

    private void performTestJobStatus(String jobId, boolean finishedSuccessfully){
        RestAssured.with()
                    .port(getServerPort())
                    .pathParam("jobId", jobId)
                .when()
                    .get(ApplicationController.ApplicationControllerMapping.JOB_STATUS_METHOD)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .body("jobFinished", CoreMatchers.notNullValue())
                    .body("finishedSuccessfully", CoreMatchers.equalTo(finishedSuccessfully))
                    .body("jobId", CoreMatchers.equalTo(jobId));
    }

    @Test
    public void testJobStatus() throws Exception {
        String jobId = waitJobFinished(scheduleJob());
        performTestJobStatus(jobId, true);
    }

    @Test
    public void testJobStatusIndicatesJobFailedWhenExceptionOccurred() throws Exception {
        Mockito.doThrow(new RuntimeException("Simulates error when executing job")).when(myJobService).doStuff();
        String jobId = waitJobFinished(scheduleJob());
        performTestJobStatus(jobId, false);
    }

    public String performTestScheduleJob() {
        Response response = RestAssured.with().port(getServerPort()).when()
                .get(ApplicationController.ApplicationControllerMapping.SCHEDULE_JOB_METHOD);
        response.then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(ContentType.TEXT.withCharset(Charset.forName("UTF-8")));
        return response.getBody().asString();
    }

    @Test
    public void testScheduleJob() throws Exception {
        String jobId = performTestScheduleJob();
        waitJobFinished(jobId);
        performTestJobStatus(jobId, true);
    }

    @Test
    public void testScheduleJobReturnsExistingJobIdWhenThereIsJobAwaitingExecution() throws Exception {
        Mockito.doAnswer(new AnswersWithDelay(3000, invocation -> null)).when(myJobService).doStuff();
        String job1 = waitJobStarted(performTestScheduleJob());//This job should be started to perform test
        String job2 = performTestScheduleJob();//This job should have ~3s before being started
        String expectedJob2Id = performTestScheduleJob();
        try {
            Assert.assertTrue(scheduler.getCurrentlyExecutingJobs().size() <= 1);
            Assert.assertEquals(job2, expectedJob2Id);
        } finally {
            waitJobFinished(job1);
            waitJobFinished(job2);
            waitJobFinished(expectedJob2Id);
        }
        performTestJobStatus(job1, true);
        performTestJobStatus(job2, true);
        performTestJobStatus(expectedJob2Id, true);
    }
}
