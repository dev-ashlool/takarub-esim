package com.takarub.esim.commerce.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.usecase.ReconcileStaleFulfillmentWorkUseCase;

/**
 * Background worker that marks stale PROCESSING fulfillment work as UNKNOWN. Does not purchase or
 * retry suppliers.
 */
@Component
@ConditionalOnProperty(
        prefix = "takarub.commerce.fulfillment-reconciliation",
        name = "enabled",
        havingValue = "true")
public class FulfillmentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentReconciliationScheduler.class);

    private final ReconcileStaleFulfillmentWorkUseCase reconcileStaleFulfillmentWorkUseCase;

    public FulfillmentReconciliationScheduler(
            ReconcileStaleFulfillmentWorkUseCase reconcileStaleFulfillmentWorkUseCase) {
        this.reconcileStaleFulfillmentWorkUseCase = reconcileStaleFulfillmentWorkUseCase;
    }

    @Scheduled(
            fixedDelayString = "${takarub.commerce.fulfillment-reconciliation.fixed-delay-ms:60000}",
            initialDelayString = "${takarub.commerce.fulfillment-reconciliation.initial-delay-ms:30000}")
    public void tick() {
        try {
            reconcileStaleFulfillmentWorkUseCase.execute();
        } catch (RuntimeException ex) {
            log.error("Unexpected failure while reconciling stale fulfillment work", ex);
        }
    }
}
