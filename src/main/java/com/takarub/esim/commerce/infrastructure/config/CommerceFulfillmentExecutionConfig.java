package com.takarub.esim.commerce.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.commerce.application.usecase.ProcessNextFulfillmentUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;

/**
 * Conditional wiring for fulfillment execution. Requires a {@link SupplierPurchasePort} bean when
 * enabled — startup fails fast if the worker is on without a purchase adapter. Does not create the
 * fake purchase adapter.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "takarub.commerce.fulfillment-execution",
        name = "enabled",
        havingValue = "true")
public class CommerceFulfillmentExecutionConfig {

    @Bean
    public ProcessNextFulfillmentUseCase processNextFulfillmentUseCase(
            TransactionRunner transactionRunner,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ProvisionedEsimRepository provisionedEsimRepository,
            SupplierPurchasePort supplierPurchasePort,
            IdGenerator idGenerator,
            ClockProvider clockProvider) {
        return new ProcessNextFulfillmentUseCase(
                transactionRunner,
                fulfillmentWorkRepository,
                provisionedEsimRepository,
                supplierPurchasePort,
                idGenerator,
                clockProvider);
    }
}
