package com.mz.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MyJobService {

    public void doStuff() throws Exception{
        log.info("Job started");
        for(int i=0; i < 100; ++i) {
            log.info("Job executing");
            Thread.sleep(600);
        }
        log.info("Job ended");
    }
}
