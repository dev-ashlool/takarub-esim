package com.takarub.esim.supplier.infrastructure.adapters.fake;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseSuccessPayload;

/**
 * Deterministic local/dev {@link SupplierPurchasePort}. Never contacts a real supplier. Gated by
 * {@code takarub.supplier.fake-purchase.enabled} independently of fulfillment worker enablement.
 */
@Component
@ConditionalOnProperty(
        prefix = "takarub.supplier.fake-purchase",
        name = "enabled",
        havingValue = "true")
public class FakeSupplierPurchaseAdapter implements SupplierPurchasePort {

    static final String CODE_REJECTED = "FAKE_SUPPLIER_REJECTED";
    static final String CODE_AMBIGUOUS = "FAKE_SUPPLIER_AMBIGUOUS";

    @Override
    public SupplierPurchaseResult purchase(SupplierPurchaseRequest request) {
        if (request == null) {
            throw new ValidationException("Supplier purchase request is required");
        }
        String remoteProductId = request.remoteProductId();
        if (remoteProductId.startsWith("FAIL_")) {
            return SupplierPurchaseResult.failed(
                    CODE_REJECTED, "Fake supplier rejected product " + remoteProductId);
        }
        if (remoteProductId.startsWith("UNKNOWN_")) {
            return SupplierPurchaseResult.unknown(
                    CODE_AMBIGUOUS, "Fake supplier ambiguous product " + remoteProductId);
        }
        String workId = request.fulfillmentWorkId();
        return SupplierPurchaseResult.succeeded(
                new SupplierPurchaseSuccessPayload(
                        "fake-order-" + workId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "LPA:1$fake.smdp$activation-" + workId));
    }
}
