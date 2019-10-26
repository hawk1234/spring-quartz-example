package com.mz.example.service;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

public class SchedulerServiceJobListenerTest extends SchedulerServiceBaseTest{

    @MockBean
    private HistoryUpdateService historyUpdateService;

    @Test
    public void testSchedulerServiceIgnoresClearingHistoryJobsWhenStoringUpdatingHistory() throws Exception {
        fireClearingHistoryJob();
        Mockito.verifyNoMoreInteractions(historyUpdateService);
    }
}
