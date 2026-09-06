package com.takarub.esim.commerce.application.usecase;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseSuccessPayload;

/**
 * Claims one PENDING fulfillment work, purchases outside the transaction, then applies the result.
 * Unexpected supplier runtime failures leave work in PROCESSING (no automatic UNKNOWN).
 *
 * <p>On successful fulfill (after TX2 commit), sends a best-effort eSIM-ready customer email.
 * Notification failures never change fulfillment outcome.
 */
public class ProcessNextFulfillmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessNextFulfillmentUseCase.class);

    private final TransactionRunner transactionRunner;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;
    private final ProvisionedEsimRepository provisionedEsimRepository;
    private final OrderRepository orderRepository;
    private final CustomerEmailLookup customerEmailLookup;
    private final EsimReadyCustomerNotifier esimReadyCustomerNotifier;
    private final SupplierPurchasePort supplierPurchasePort;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public ProcessNextFulfillmentUseCase(
            TransactionRunner transactionRunner,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ProvisionedEsimRepository provisionedEsimRepository,
            OrderRepository orderRepository,
            CustomerEmailLookup customerEmailLookup,
            EsimReadyCustomerNotifier esimReadyCustomerNotifier,
            SupplierPurchasePort supplierPurchasePort,
            IdGenerator idGenerator,
            ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
        this.provisionedEsimRepository = provisionedEsimRepository;
        this.orderRepository = orderRepository;
        this.customerEmailLookup = customerEmailLookup;
        this.esimReadyCustomerNotifier = esimReadyCustomerNotifier;
        this.supplierPurchasePort = supplierPurchasePort;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public void execute() {
        Optional<FulfillmentWork> claimed =
                transactionRunner.execute(() -> fulfillmentWorkRepository.claimNextPending(clock));
        if (claimed.isEmpty()) {
            return;
        }

        FulfillmentWork claimedWork = claimed.get();
        SupplierPurchaseRequest request = new SupplierPurchaseRequest(
                claimedWork.supplierKey(),
                claimedWork.remoteProductId(),
                claimedWork.id().value().toString());

        SupplierPurchaseResult result = supplierPurchasePort.purchase(request);

        // TX2: persist ProvisionedEsim + FULFILLED. Notifier runs only after this execute returns
        // (TransactionTemplate commit), never inside the callback.
        Optional<OrderId> fulfilledOrderId = transactionRunner.execute(
                () -> applyPurchaseResult(claimedWork.id(), result));

        fulfilledOrderId.ifPresent(this::notifyEsimReadyBestEffort);
    }

    /**
     * @return order id when SUCCEEDED and persisted; empty otherwise
     */
    private Optional<OrderId> applyPurchaseResult(FulfillmentId workId, SupplierPurchaseResult result) {
        FulfillmentWork work = fulfillmentWorkRepository
                .findById(workId)
                .orElseThrow(() -> new ConflictException(
                        "Claimed fulfillment work missing on reload: " + workId.value()));

        if (work.status() != FulfillmentStatus.PROCESSING) {
            return Optional.empty();
        }

        switch (result.outcome()) {
            case SUCCEEDED -> {
                SupplierPurchaseSuccessPayload payload = result.successPayload();
                ProvisionedEsim esim = ProvisionedEsim.create(
                        idGenerator,
                        clock,
                        work.orderId(),
                        work.id(),
                        work.supplierKey(),
                        work.remoteProductId(),
                        payload.supplierOrderId(),
                        payload.iccid(),
                        payload.smdpAddress(),
                        payload.activationCode(),
                        payload.pin(),
                        payload.puk(),
                        payload.qrString());
                provisionedEsimRepository.save(esim);
                work.markFulfilled(clock);
                fulfillmentWorkRepository.save(work);
                return Optional.of(work.orderId());
            }
            case FAILED -> {
                work.markBlocked(clock, result.errorCode(), result.errorMessage());
                fulfillmentWorkRepository.save(work);
                return Optional.empty();
            }
            case UNKNOWN -> {
                work.markUnknown(clock, result.errorCode(), result.errorMessage());
                fulfillmentWorkRepository.save(work);
                return Optional.empty();
            }
            default -> throw new ConflictException(
                    "Unsupported supplier purchase outcome: " + result.outcome());
        }
    }

    private void notifyEsimReadyBestEffort(OrderId orderId) {
        try {
            Optional<Order> order = orderRepository.findById(orderId);
            if (order.isEmpty()) {
                log.warn("eSIM ready notification skipped: order missing after fulfill orderId={}",
                        orderId.value());
                return;
            }
            Optional<EmailAddress> email = customerEmailLookup.findEmailByUserId(order.get().userId());
            if (email.isEmpty()) {
                log.warn("eSIM ready notification skipped: customer email unavailable orderId={}",
                        orderId.value());
                return;
            }
            esimReadyCustomerNotifier.notifyEsimReady(orderId, email.get());
        } catch (RuntimeException ex) {
            log.error("eSIM ready notification failed after fulfill orderId={}", orderId.value(), ex);
        }
    }
}
