package com.takarub.esim.commerce.domain.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class ProvisionedEsimTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final SystemClockProvider clock =
            new SystemClockProvider(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createAcceptsQrProof() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        FulfillmentId workId = FulfillmentId.of(UUID.randomUUID());

        ProvisionedEsim esim = ProvisionedEsim.create(
                idGenerator,
                clock,
                orderId,
                workId,
                "LIKE_CARD",
                "5653",
                "sup-1",
                null,
                null,
                null,
                null,
                null,
                "LPA:1$fake.smdp$activation-1");

        assertThat(esim.orderId()).isEqualTo(orderId);
        assertThat(esim.fulfillmentWorkId()).isEqualTo(workId);
        assertThat(esim.supplierOrderId()).isEqualTo("sup-1");
        assertThat(esim.qrString()).isEqualTo("LPA:1$fake.smdp$activation-1");
        assertThat(esim.createdAt()).isEqualTo(NOW);
        assertThat(esim.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createAcceptsSmdpAndActivationCode() {
        ProvisionedEsim esim = ProvisionedEsim.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                "sup-1",
                "8901",
                "smdp.example",
                "ACT-1",
                "1234",
                "5678",
                null);

        assertThat(esim.smdpAddress()).isEqualTo("smdp.example");
        assertThat(esim.activationCode()).isEqualTo("ACT-1");
        assertThat(esim.iccid()).isEqualTo("8901");
        assertThat(esim.pin()).isEqualTo("1234");
        assertThat(esim.puk()).isEqualTo("5678");
        assertThat(esim.qrString()).isNull();
    }

    @Test
    void createRejectsIccidOnly() {
        assertThatThrownBy(() -> ProvisionedEsim.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                "sup-1",
                "8901",
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("activation proof");
    }

    @Test
    void createRejectsMissingSupplierOrderId() {
        assertThatThrownBy(() -> ProvisionedEsim.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                " ",
                null,
                null,
                null,
                null,
                null,
                "LPA:1$x$y"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierOrderId");
    }

    @Test
    void createRejectsMissingSupplierKey() {
        assertThatThrownBy(() -> ProvisionedEsim.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                null,
                "5653",
                "sup-1",
                null,
                null,
                null,
                null,
                null,
                "LPA:1$x$y"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierKey");
    }

    @Test
    void reconstituteNormalizesBlankOptionalFields() {
        ProvisionedEsim esim = ProvisionedEsim.reconstitute(
                ProvisionedEsimId.of(UUID.randomUUID()),
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                "sup-1",
                "  ",
                "  ",
                "  ",
                "  ",
                "  ",
                "LPA:1$x$y",
                NOW,
                NOW);

        assertThat(esim.iccid()).isNull();
        assertThat(esim.smdpAddress()).isNull();
        assertThat(esim.activationCode()).isNull();
        assertThat(esim.pin()).isNull();
        assertThat(esim.puk()).isNull();
        assertThat(esim.qrString()).isEqualTo("LPA:1$x$y");
    }
}
