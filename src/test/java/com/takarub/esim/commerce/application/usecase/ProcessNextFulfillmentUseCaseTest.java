package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.port.CustomerEmailLookup;
import com.takarub.esim.commerce.application.port.EsimReadyCustomerNotifier;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;
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
    private static final EmailAddress CUSTOMER_EMAIL = EmailAddress.of("customer@example.com");
    private static final UserId USER_ID = UserId.of(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ProvisionedEsimRepository provisionedEsimRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerEmailLookup customerEmailLookup;
    @Mock
    private EsimReadyCustomerNotifier esimReadyCustomerNotifier;
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
                orderRepository,
                customerEmailLookup,
                esimReadyCustomerNotifier,
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
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
    }

    @Test
    void supplierPurchaseOccursAfterTx1Returns() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        AtomicInteger purchaseBeforeSecondTx = new AtomicInteger();
        AtomicInteger txCount = new AtomicInteger();
        stubSuccessfulNotifyPath(claimed);

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
        stubSuccessfulNotifyPath(claimed);
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
    void succeededFulfillmentNotifiesEsimReadyExactlyOnceAfterTx2() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        stubSuccessfulNotifyPath(claimed);
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        AtomicInteger txCount = new AtomicInteger();
        AtomicBoolean notifiedDuringTx2 = new AtomicBoolean(false);
        org.mockito.Mockito.doAnswer(invocation -> {
                    int n = txCount.incrementAndGet();
                    Supplier<?> work = invocation.getArgument(0);
                    Object result = work.get();
                    if (n == 2) {
                        // Still inside TransactionRunner.execute — commit has not returned yet.
                        boolean alreadyNotified = org.mockito.Mockito.mockingDetails(esimReadyCustomerNotifier)
                                .getInvocations().stream()
                                .anyMatch(inv -> inv.getMethod().getName().equals("notifyEsimReady"));
                        notifiedDuringTx2.set(alreadyNotified);
                    }
                    return result;
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());

        useCase.execute();

        assertThat(notifiedDuringTx2.get()).isFalse();
        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.FULFILLED);
        verify(provisionedEsimRepository).save(any());
        verify(esimReadyCustomerNotifier, times(1)).notifyEsimReady(eq(claimed.orderId()), eq(CUSTOMER_EMAIL));
    }

    @Test
    void notifierFailureDoesNotChangeFulfilledOrEscape() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        stubSuccessfulNotifyPath(claimed);
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(esimReadyCustomerNotifier).notifyEsimReady(any(), any());

        assertThatCode(useCase::execute).doesNotThrowAnyException();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.FULFILLED);
        verify(provisionedEsimRepository).save(any());
        verify(esimReadyCustomerNotifier).notifyEsimReady(eq(claimed.orderId()), eq(CUSTOMER_EMAIL));
    }

    @Test
    void failedSupplierOutcomeNeverNotifies() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "FAIL_X");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any()))
                .thenReturn(SupplierPurchaseResult.failed("REJECTED", "no"));

        useCase.execute();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void unknownSupplierOutcomeNeverNotifies() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "UNKNOWN_X");
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any()))
                .thenReturn(SupplierPurchaseResult.unknown("AMBIGUOUS", "maybe"));

        useCase.execute();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.UNKNOWN);
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
    }

    @Test
    void missingCustomerEmailKeepsFulfilledAndSkipsNotifier() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        Order order = mock(Order.class);
        when(order.userId()).thenReturn(USER_ID);
        when(orderRepository.findById(claimed.orderId())).thenReturn(Optional.of(order));
        when(customerEmailLookup.findEmailByUserId(USER_ID)).thenReturn(Optional.empty());
        when(fulfillmentWorkRepository.claimNextPending(clock)).thenReturn(Optional.of(claimed));
        when(fulfillmentWorkRepository.findById(claimed.id())).thenReturn(Optional.of(claimed));
        when(supplierPurchasePort.purchase(any())).thenReturn(succeeded(claimed));

        assertThatCode(useCase::execute).doesNotThrowAnyException();

        assertThat(claimed.status()).isEqualTo(FulfillmentStatus.FULFILLED);
        verify(provisionedEsimRepository).save(any());
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
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
        stubSuccessfulNotifyPath(claimed);
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
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
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
        verify(esimReadyCustomerNotifier, never()).notifyEsimReady(any(), any());
    }

    @Test
    void passesFrozenSupplierKeyProductAndCorrelationId() {
        FulfillmentWork claimed = processingWork("LIKE_CARD", "5653");
        stubSuccessfulNotifyPath(claimed);
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

    private void stubSuccessfulNotifyPath(FulfillmentWork claimed) {
        Order order = mock(Order.class);
        when(order.userId()).thenReturn(USER_ID);
        when(orderRepository.findById(claimed.orderId())).thenReturn(Optional.of(order));
        when(customerEmailLookup.findEmailByUserId(USER_ID)).thenReturn(Optional.of(CUSTOMER_EMAIL));
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
