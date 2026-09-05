package com.takarub.esim.supplier.infrastructure.adapters.fake;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseOutcome;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

class FakeSupplierPurchaseAdapterTest {

    private final FakeSupplierPurchaseAdapter adapter = new FakeSupplierPurchaseAdapter();

    @Test
    void defaultSuccess() {
        String workId = UUID.randomUUID().toString();
        SupplierPurchaseResult result = adapter.purchase(
                new SupplierPurchaseRequest("LIKE_CARD", "5653", workId));

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.SUCCEEDED);
        assertThat(result.successPayload().supplierOrderId()).isEqualTo("fake-order-" + workId);
        assertThat(result.successPayload().qrString())
                .isEqualTo("LPA:1$fake.smdp$activation-" + workId);
    }

    @Test
    void failPrefix() {
        SupplierPurchaseResult result = adapter.purchase(
                new SupplierPurchaseRequest("LIKE_CARD", "FAIL_X", UUID.randomUUID().toString()));

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(FakeSupplierPurchaseAdapter.CODE_REJECTED);
    }

    @Test
    void unknownPrefix() {
        SupplierPurchaseResult result = adapter.purchase(
                new SupplierPurchaseRequest("LIKE_CARD", "UNKNOWN_X", UUID.randomUUID().toString()));

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.UNKNOWN);
        assertThat(result.errorCode()).isEqualTo(FakeSupplierPurchaseAdapter.CODE_AMBIGUOUS);
    }

    @Test
    void successPayloadSatisfiesProvisioningInvariant() {
        String workId = UUID.randomUUID().toString();
        SupplierPurchaseResult result = adapter.purchase(
                new SupplierPurchaseRequest("LIKE_CARD", "5653", workId));

        Instant now = Instant.parse("2026-09-05T12:00:00Z");
        ProvisionedEsim esim = ProvisionedEsim.create(
                new UuidIdGenerator(),
                new SystemClockProvider(Clock.fixed(now, ZoneOffset.UTC)),
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.fromString(workId)),
                "LIKE_CARD",
                "5653",
                result.successPayload().supplierOrderId(),
                result.successPayload().iccid(),
                result.successPayload().smdpAddress(),
                result.successPayload().activationCode(),
                result.successPayload().pin(),
                result.successPayload().puk(),
                result.successPayload().qrString());

        assertThat(esim.qrString()).isNotBlank();
        assertThat(esim.supplierOrderId()).startsWith("fake-order-");
    }
}
