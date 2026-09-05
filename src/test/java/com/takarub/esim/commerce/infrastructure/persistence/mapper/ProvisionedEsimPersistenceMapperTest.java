package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.infrastructure.persistence.entity.ProvisionedEsimJpaEntity;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class ProvisionedEsimPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    private final ProvisionedEsimPersistenceMapper mapper = new ProvisionedEsimPersistenceMapper();
    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final SystemClockProvider clock =
            new SystemClockProvider(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void fullRoundTrip() {
        ProvisionedEsim original = ProvisionedEsim.create(
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
                "LPA:1$smdp$act");

        ProvisionedEsimJpaEntity entity = mapper.toEntity(original);
        ProvisionedEsim restored = mapper.toDomain(entity);

        assertThat(entity.getOrderId()).isEqualTo(original.orderId().value().toString());
        assertThat(entity.getFulfillmentWorkId())
                .isEqualTo(original.fulfillmentWorkId().value().toString());
        assertThat(entity.getSupplierOrderId()).isEqualTo("sup-1");
        assertThat(entity.getIccid()).isEqualTo("8901");
        assertThat(entity.getQrString()).isEqualTo("LPA:1$smdp$act");
        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(restored.pin()).isEqualTo("1234");
        assertThat(restored.puk()).isEqualTo("5678");
    }

    @Test
    void nullableOptionalFieldsRoundTrip() {
        ProvisionedEsim original = ProvisionedEsim.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                FulfillmentId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                "sup-1",
                null,
                null,
                null,
                null,
                null,
                "LPA:1$fake$x");

        ProvisionedEsimJpaEntity entity = mapper.toEntity(original);
        ProvisionedEsim restored = mapper.toDomain(entity);

        assertThat(entity.getIccid()).isNull();
        assertThat(entity.getSmdpAddress()).isNull();
        assertThat(entity.getActivationCode()).isNull();
        assertThat(entity.getPin()).isNull();
        assertThat(entity.getPuk()).isNull();
        assertThat(restored.iccid()).isNull();
        assertThat(restored.smdpAddress()).isNull();
        assertThat(restored.activationCode()).isNull();
        assertThat(restored.pin()).isNull();
        assertThat(restored.puk()).isNull();
        assertThat(restored.qrString()).isEqualTo("LPA:1$fake$x");
    }
}
