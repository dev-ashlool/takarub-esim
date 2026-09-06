package com.takarub.esim.commerce.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.commerce.application.usecase.ReconcileStaleFulfillmentWorkUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Conditional wiring for conservative fulfillment reconciliation. Independent of purchase execution
 * and supplier adapters. Marks stale PROCESSING as UNKNOWN only — never retries supplier purchase.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "takarub.commerce.fulfillment-reconciliation",
        name = "enabled",
        havingValue = "true")
public class CommerceFulfillmentReconciliationConfig {

    static final String STALE_PROCESSING_THRESHOLD_PROPERTY =
            "takarub.commerce.fulfillment-reconciliation.stale-processing-threshold";

    @Bean
    public ReconcileStaleFulfillmentWorkUseCase reconcileStaleFulfillmentWorkUseCase(
            TransactionRunner transactionRunner,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ClockProvider clockProvider,
            @Value("${takarub.commerce.fulfillment-reconciliation.stale-processing-threshold:PT30M}")
                    String staleProcessingThreshold) {
        return new ReconcileStaleFulfillmentWorkUseCase(
                transactionRunner,
                fulfillmentWorkRepository,
                clockProvider,
                parsePositiveStaleProcessingThreshold(staleProcessingThreshold));
    }

    /**
     * Parses ISO-8601 duration and requires a strictly positive value ({@code > PT0S}).
     */
    static Duration parsePositiveStaleProcessingThreshold(String raw) {
        Duration threshold = Duration.parse(raw);
        if (!threshold.isPositive()) {
            throw new IllegalArgumentException(
                    STALE_PROCESSING_THRESHOLD_PROPERTY
                            + " must be strictly positive (> PT0S), got: "
                            + raw);
        }
        return threshold;
    }
}
