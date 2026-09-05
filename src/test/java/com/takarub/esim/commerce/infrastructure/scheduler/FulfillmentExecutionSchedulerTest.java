package com.takarub.esim.commerce.infrastructure.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.usecase.ProcessNextFulfillmentUseCase;

@ExtendWith(MockitoExtension.class)
class FulfillmentExecutionSchedulerTest {

    @Mock
    private ProcessNextFulfillmentUseCase processNextFulfillmentUseCase;

    @InjectMocks
    private FulfillmentExecutionScheduler scheduler;

    @Test
    void tickInvokesUseCaseOnce() {
        scheduler.tick();
        verify(processNextFulfillmentUseCase, times(1)).execute();
    }

    @Test
    void tickCatchesRuntimeException() {
        doThrow(new RuntimeException("boom")).when(processNextFulfillmentUseCase).execute();
        scheduler.tick();
        verify(processNextFulfillmentUseCase).execute();
    }
}
