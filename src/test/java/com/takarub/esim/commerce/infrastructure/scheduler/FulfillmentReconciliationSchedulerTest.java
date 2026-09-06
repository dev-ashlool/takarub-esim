package com.takarub.esim.commerce.infrastructure.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.usecase.ReconcileStaleFulfillmentWorkUseCase;

@ExtendWith(MockitoExtension.class)
class FulfillmentReconciliationSchedulerTest {

    @Mock
    private ReconcileStaleFulfillmentWorkUseCase reconcileStaleFulfillmentWorkUseCase;

    @InjectMocks
    private FulfillmentReconciliationScheduler scheduler;

    @Test
    void tickInvokesUseCaseOnce() {
        scheduler.tick();
        verify(reconcileStaleFulfillmentWorkUseCase, times(1)).execute();
    }

    @Test
    void tickCatchesRuntimeException() {
        doThrow(new RuntimeException("boom")).when(reconcileStaleFulfillmentWorkUseCase).execute();
        scheduler.tick();
        verify(reconcileStaleFulfillmentWorkUseCase).execute();
    }
}
