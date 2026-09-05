package com.takarub.esim.commerce.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.takarub.esim.commerce.application.usecase.ProcessNextFulfillmentUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.commerce.infrastructure.scheduler.FulfillmentExecutionScheduler;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;
import com.takarub.esim.supplier.infrastructure.adapters.fake.FakeSupplierPurchaseAdapter;

class CommerceFulfillmentExecutionConfigTest {

    private final ApplicationContextRunner baseRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    CommerceFulfillmentExecutionConfig.class,
                    FakeSupplierPurchaseAdapter.class,
                    FulfillmentExecutionScheduler.class)
            .withBean(TransactionRunner.class, () -> mock(TransactionRunner.class))
            .withBean(FulfillmentWorkRepository.class, () -> mock(FulfillmentWorkRepository.class))
            .withBean(ProvisionedEsimRepository.class, () -> mock(ProvisionedEsimRepository.class))
            .withBean(IdGenerator.class, () -> mock(IdGenerator.class))
            .withBean(ClockProvider.class, () -> mock(ClockProvider.class));

    @Test
    void workerDisabledFakeDisabledStartsCleanWithoutWorkerUseCase() {
        baseRunner
                .withPropertyValues(
                        "takarub.commerce.fulfillment-execution.enabled=false",
                        "takarub.supplier.fake-purchase.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProcessNextFulfillmentUseCase.class);
                    assertThat(context).doesNotHaveBean(FulfillmentExecutionScheduler.class);
                    assertThat(context).doesNotHaveBean(FakeSupplierPurchaseAdapter.class);
                    assertThat(context).doesNotHaveBean(SupplierPurchasePort.class);
                });
    }

    @Test
    void workerDisabledFakeEnabledMayHaveFakeButNoWorker() {
        baseRunner
                .withPropertyValues(
                        "takarub.commerce.fulfillment-execution.enabled=false",
                        "takarub.supplier.fake-purchase.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FakeSupplierPurchaseAdapter.class);
                    assertThat(context).hasSingleBean(SupplierPurchasePort.class);
                    assertThat(context).doesNotHaveBean(ProcessNextFulfillmentUseCase.class);
                    assertThat(context).doesNotHaveBean(FulfillmentExecutionScheduler.class);
                });
    }

    @Test
    void workerEnabledFakeEnabledWiresWorkerAndFake() {
        baseRunner
                .withPropertyValues(
                        "takarub.commerce.fulfillment-execution.enabled=true",
                        "takarub.supplier.fake-purchase.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FakeSupplierPurchaseAdapter.class);
                    assertThat(context).hasSingleBean(ProcessNextFulfillmentUseCase.class);
                    assertThat(context).hasSingleBean(FulfillmentExecutionScheduler.class);
                });
    }

    @Test
    void workerEnabledWithoutPurchasePortFailsFast() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        CommerceFulfillmentExecutionConfig.class,
                        FulfillmentExecutionScheduler.class)
                .withBean(TransactionRunner.class, () -> mock(TransactionRunner.class))
                .withBean(FulfillmentWorkRepository.class, () -> mock(FulfillmentWorkRepository.class))
                .withBean(ProvisionedEsimRepository.class, () -> mock(ProvisionedEsimRepository.class))
                .withBean(IdGenerator.class, () -> mock(IdGenerator.class))
                .withBean(ClockProvider.class, () -> mock(ClockProvider.class))
                .withPropertyValues(
                        "takarub.commerce.fulfillment-execution.enabled=true",
                        "takarub.supplier.fake-purchase.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }
}
