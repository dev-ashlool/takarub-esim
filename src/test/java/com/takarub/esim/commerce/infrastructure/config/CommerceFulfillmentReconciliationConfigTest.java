package com.takarub.esim.commerce.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.DateTimeException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.takarub.esim.commerce.application.usecase.ReconcileStaleFulfillmentWorkUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.infrastructure.scheduler.FulfillmentReconciliationScheduler;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.time.ClockProvider;

class CommerceFulfillmentReconciliationConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    CommerceFulfillmentReconciliationConfig.class,
                    FulfillmentReconciliationScheduler.class)
            .withBean(TransactionRunner.class, () -> mock(TransactionRunner.class))
            .withBean(FulfillmentWorkRepository.class, () -> mock(FulfillmentWorkRepository.class))
            .withBean(ClockProvider.class, () -> mock(ClockProvider.class));

    @Test
    void featureOffByDefaultWiresNothing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(ReconcileStaleFulfillmentWorkUseCase.class);
            assertThat(context).doesNotHaveBean(FulfillmentReconciliationScheduler.class);
        });
    }

    @Test
    void featureOnWiresUseCaseAndSchedulerWithoutSupplierBeans() {
        runner.withPropertyValues(
                        "takarub.commerce.fulfillment-reconciliation.enabled=true",
                        "takarub.commerce.fulfillment-reconciliation.stale-processing-threshold=PT15M")
                .run(context -> {
                    assertThat(context).hasSingleBean(ReconcileStaleFulfillmentWorkUseCase.class);
                    assertThat(context).hasSingleBean(FulfillmentReconciliationScheduler.class);
                });
    }

    @Test
    void defaultThresholdPt30mIsAcceptedWhenPropertyAbsent() {
        runner.withPropertyValues("takarub.commerce.fulfillment-reconciliation.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ReconcileStaleFulfillmentWorkUseCase.class));
    }

    @Test
    void zeroThresholdIsRejected() {
        runner.withPropertyValues(
                        "takarub.commerce.fulfillment-reconciliation.enabled=true",
                        "takarub.commerce.fulfillment-reconciliation.stale-processing-threshold=PT0S")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void negativeThresholdIsRejected() {
        runner.withPropertyValues(
                        "takarub.commerce.fulfillment-reconciliation.enabled=true",
                        "takarub.commerce.fulfillment-reconciliation.stale-processing-threshold=-PT5M")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void invalidThresholdSyntaxIsRejected() {
        runner.withPropertyValues(
                        "takarub.commerce.fulfillment-reconciliation.enabled=true",
                        "takarub.commerce.fulfillment-reconciliation.stale-processing-threshold=not-a-duration")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void parsePositiveAcceptsDefaultAndOtherPositiveValues() {
        assertThat(CommerceFulfillmentReconciliationConfig.parsePositiveStaleProcessingThreshold("PT30M"))
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(CommerceFulfillmentReconciliationConfig.parsePositiveStaleProcessingThreshold("PT15M"))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void parsePositiveRejectsZeroNegativeAndInvalid() {
        assertThatThrownBy(() -> CommerceFulfillmentReconciliationConfig.parsePositiveStaleProcessingThreshold(
                        "PT0S"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        CommerceFulfillmentReconciliationConfig.STALE_PROCESSING_THRESHOLD_PROPERTY);
        assertThatThrownBy(() -> CommerceFulfillmentReconciliationConfig.parsePositiveStaleProcessingThreshold(
                        "-PT1S"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        CommerceFulfillmentReconciliationConfig.STALE_PROCESSING_THRESHOLD_PROPERTY);
        assertThatThrownBy(() -> CommerceFulfillmentReconciliationConfig.parsePositiveStaleProcessingThreshold(
                        "bogus"))
                .isInstanceOf(DateTimeException.class);
    }
}
