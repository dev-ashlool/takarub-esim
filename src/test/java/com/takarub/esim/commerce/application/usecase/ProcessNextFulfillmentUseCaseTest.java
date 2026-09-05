package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseSuccessPayload;

@ExtendWith(MockitoExtension.class)
class ProcessNextFulfillmentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final Instant CLAIMED = Instant.parse("2026-09-05T12:05:00Z");

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ProvisionedEsimRepository provisionedEsimRepository;
    @Mock
    private SupplierPurchasePort supplierPurchasePort;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private ProcessNextFulfillmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessNextFulfillmentUseCase(
                transactionRunner,
                fulfillmentWorkRepository,
                provisionedEsimRepository,
                supplierPurchasePort,
                idGenerator,
                clock);
        lenient().doAnswer(invocation -> {
                    Supplier<?> work = invocation.getArgument(0);
                    return work.get();
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(fulfillmentWorkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(provisionedEsimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void emptyQueueDoesNotCallPurchase() {
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.empty());

        useCase.execute();

        verify(supplierPurchasePort, never()).purchase(any());
        verify(transactionRunner, times(1)).execute(any());
    }

    @Test
    void supplierPurchaseOccursAfterTx1Returns() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        AtomicInteger purchaseBeforeSecondTx = new AtomicInteger();
        AtomicInteger txCount = new AtomicInteger();

        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        org.mockito.Mockito.doAnswer(invocation -> {
                    int n = txCount.incrementAndGet();
                    if (n == 2) {
                        purchaseBeforeSecondTx.set(
                                org.mockito.Mockito.mockingDetails(supplierPurchasePort)
                                        .getInvocations().stream()
                                        .filter(inv -> inv.getMethod().getName().equals("purchase"))
                                        .mapToInt(inv -> 1)
                                        .sum());
                    }
                    Supplier<?> work = invocation.getArgument(0);
                    return work.get();
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());

        useCase.execute();

        assertThat(purchaseBeforeSecondTx.get()).isEqualTo(1);
        verify(transactionRunner, times(2)).execute(any());
    }

    @Test
    void successCreatesProvisioningAndMarksFulfilledInTx2() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        useCase.execute();

        ArgumentCaptor<ProvisionedEsim> esimCaptor = ArgumentCaptor.forClass(ProvisionedEsim.class);
        verify(provisionedEsimRepository).save(esimCaptor.capture());
        ProvisionedEsim esim = esimCaptor.getValue();
        assertThat(esim.orderId()).isEqualTo(claimed.orderId());
        assertThat(esim.fulfillmentWorkId()).isEqualTo(claimed.id());
        assertThat(esim.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(esim.remoteProductId()).isEqualTo("5653");
        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.FULFILLED);
        verify(fulfillmentWorkRepository).save(claimed);
    }

    @Test
    void failedMarksBlocked() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "FAIL_X");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any()))
                .thenReturn(SupplierPurchaseResult.failed("REJECTED", "no"));

        useCase.execute();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(claimed.lastErrorCode()).isEqualTo("REJECTED");
        verify(provisionedEsimRepository, never()).save(any());
        verify(fulfillmentWorkRepository).save(claimed);
    }

    @Test
    void unknownMarksUnknown() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "UNKNOWN_X");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any()))
                .thenReturn(SupplierPurchaseResult.unknown("AMBIGUOUS", "maybe"));

        useCase.execute();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.UNKNOWN);
        assertThat(claimed.lastErrorCode()).isEqualTo("AMBIGUOUS");
        verify(provisionedEsimRepository, never()).save(any());
    }

    @Test
    void tx2ReloadsById() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        useCase.execute();

        InOrder order = inOrder(fulfillmentWorkRepository, supplierPurchasePort);
        order.verify(fulfillmentWorkRepository).claimNextPending(clock);
        order.verify(supplierPurchasePort).purchase(any());
        order.verify(fulfillmentWorkRepository).findById(claimed.id());
    }

    @Test
    void unexpectedSupplierExceptionSkipsTx2Apply() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(useCase::execute).isInstanceOf(RuntimeException.class).hasMessage("boom");

        verify(fulfillmentWorkRepository, never()).findById(any());
        verify(provisionedEsimRepository, never()).save(any());
        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.PROCESSING);
        verify(transactionRunner, times(1)).execute(any());
    }

    @Test
    void nonProcessingOnReloadIsSkippedSafely() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        FulfillmentWork alreadyDone = FulfillmentWork.reconstitute(
                claimed.id(),
                claimed.orderId(),
                "LIKE_CARD",
                "5653",
                FulfillmentStatus.FULFILLED,
                CLAIMED,
                null,
                null,
                NOW,
                NOW);
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(alreadyDone));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        useCase.execute();

        verify(provisionedEsimRepository, never()).save(any());
        verify(fulfillmentWorkRepository, never()).save(any());
    }

    @Test
    void passesFrozenSupplierKeyProductAndCorrelationId() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        useCase.execute();

        ArgumentCaptor<SupplierPurchaseRequest> requestCaptor =
                ArgumentCaptor.forClass(SupplierPurchaseRequest.class);
        verify(supplierPurchasePort).purchase(requestCaptor.capture());
        SupplierPurchaseRequest request = requestCaptor.getValue();
        assertThat(request.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(request.remoteProductId()).isEqualTo("5653");
        assertThat(request.fulfillmentWorkId()).isEqualTo(claimed.id().value().toString());
    }

    private FulfillmentWork processingWork(String supplierKey, String remoteProductId) {
        return FulfillmentWork.reconstitute(
                FulfillmentId.of(UUID.randomUUID()),
                OrderId.of(UUID.randomUUID()),
                supplierKey,
                remoteProductId,
                FulfillmentStatus.PROCESSING,
                CLAIMED,
                null,
                null,
                NOW,
                NOW);
    }

    private static SupplierPurchaseResult succeeded(FulfillmentWork work) {
        return SupplierPurchaseResult.succeeded(
                new SupplierPurchaseSuccessPayload(
                        "fake-order-" + work.id().value(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "LPA:1$fake.smdp$activation-" + work.id().value()));
    }
}
