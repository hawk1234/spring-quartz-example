package com.mz.example.config;

import com.mz.example.quartz.ClearingHistoryJob;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobHistoryConfiguration {

    @Bean
    @ConfigurationProperties("com.mz.example.job-history")
    public ClearingHistoryJob.Config clearingHistoryConfig() {
        return new ClearingHistoryJob.Config();
    }
}
