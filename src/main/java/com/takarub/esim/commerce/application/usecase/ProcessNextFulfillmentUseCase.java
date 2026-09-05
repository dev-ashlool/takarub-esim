package com.takarub.esim.commerce.application.usecase;

import java.util.Optional;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
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
 */
public class ProcessNextFulfillmentUseCase {

    private final TransactionRunner transactionRunner;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;
    private final ProvisionedEsimRepository provisionedEsimRepository;
    private final SupplierPurchasePort supplierPurchasePort;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public ProcessNextFulfillmentUseCase(
            TransactionRunner transactionRunner,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            ProvisionedEsimRepository provisionedEsimRepository,
            SupplierPurchasePort supplierPurchasePort,
            IdGenerator idGenerator,
            ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
        this.provisionedEsimRepository = provisionedEsimRepository;
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

        transactionRunner.execute(() -> {
            applyPurchaseResult(claimedWork.id(), result);
            return null;
        });
    }

    private void applyPurchaseResult(FulfillmentId workId, SupplierPurchaseResult result) {
        FulfillmentWork work = fulfillmentWorkRepository
                .findById(workId)
                .orElseThrow(() -> new ConflictException(
                        "Claimed fulfillment work missing on reload: " + workId.value()));

        if (work.status() != FulfillmentStatus.PROCESSING) {
            return;
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
            }
            case FAILED -> {
                work.markBlocked(clock, result.errorCode(), result.errorMessage());
                fulfillmentWorkRepository.save(work);
            }
            case UNKNOWN -> {
                work.markUnknown(clock, result.errorCode(), result.errorMessage());
                fulfillmentWorkRepository.save(work);
            }
            default -> throw new ConflictException(
                    "Unsupported supplier purchase outcome: " + result.outcome());
        }
    }
}
