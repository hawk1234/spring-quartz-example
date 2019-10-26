package com.mz.example.quartz;

import com.mz.example.quartz.history.repository.JobStatusRepository;
import lombok.Getter;
import lombok.Setter;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@DisallowConcurrentExecution
public final class ClearingHistoryJob extends QuartzJobBean {

    @Autowired
    private Config clearingHistoryConfig;
    @Autowired
    private JobStatusRepository jobStatusRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            ZonedDateTime expiryDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                    .minusDays(clearingHistoryConfig.getDaysToExpiry());
            jobStatusRepository.deleteByJobCreatedBefore(expiryDate);
        } catch (Throwable ex) {
            throw new JobExecutionException(ex);
        }
    }

    @Setter
    @Getter
    public static class Config {

        private int daysToExpiry;
    }
}
