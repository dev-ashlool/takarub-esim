package com.takarub.esim.commerce.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.usecase.ProcessNextFulfillmentUseCase;

/**
 * Background worker that processes at most one fulfillment work item per tick.
 */
@Component
@ConditionalOnProperty(
        prefix = "takarub.commerce.fulfillment-execution",
        name = "enabled",
        havingValue = "true")
public class FulfillmentExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentExecutionScheduler.class);

    private final ProcessNextFulfillmentUseCase processNextFulfillmentUseCase;

    public FulfillmentExecutionScheduler(ProcessNextFulfillmentUseCase processNextFulfillmentUseCase) {
        this.processNextFulfillmentUseCase = processNextFulfillmentUseCase;
    }

    @Scheduled(
            fixedDelayString = "${takarub.commerce.fulfillment-execution.fixed-delay-ms:5000}",
            initialDelayString = "${takarub.commerce.fulfillment-execution.initial-delay-ms:15000}")
    public void tick() {
        try {
            processNextFulfillmentUseCase.execute();
        } catch (RuntimeException ex) {
            log.error("Unexpected failure while processing next fulfillment work", ex);
        }
    }
}
