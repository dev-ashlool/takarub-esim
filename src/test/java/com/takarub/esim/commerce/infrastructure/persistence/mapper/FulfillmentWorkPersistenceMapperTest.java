package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class FulfillmentWorkPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    private final FulfillmentWorkPersistenceMapper mapper = new FulfillmentWorkPersistenceMapper();
    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final SystemClockProvider clock =
            new SystemClockProvider(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void roundTripsPendingWork() {
        FulfillmentWork original = FulfillmentWork.pending(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653");

        FulfillmentWorkJpaEntity entity = mapper.toEntity(original);
        FulfillmentWork restored = mapper.toDomain(entity);

        assertThat(entity.getOrderId()).isEqualTo(original.orderId().value().toString());
        assertThat(entity.getStatus()).isEqualTo(FulfillmentStatus.PENDING);
        assertThat(entity.getSupplierKey()).isEqualTo("LIKE_CARD");
        assertThat(entity.getRemoteProductId()).isEqualTo("5653");
        assertThat(entity.getLastErrorCode()).isNull();
        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.status()).isEqualTo(FulfillmentStatus.PENDING);
        assertThat(restored.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(restored.remoteProductId()).isEqualTo("5653");
    }

    @Test
    void roundTripsBlockedWorkWithoutSupplierSourcing() {
        FulfillmentWork original = FulfillmentWork.blocked(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                null,
                null,
                "LEGACY_SUPPLIER_SELECTION_MISSING",
                "Legacy paid order has no frozen supplier selection.");

        FulfillmentWorkJpaEntity entity = mapper.toEntity(original);
        FulfillmentWork restored = mapper.toDomain(entity);

        assertThat(entity.getSupplierKey()).isNull();
        assertThat(entity.getRemoteProductId()).isNull();
        assertThat(entity.getStatus()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(entity.getLastErrorCode()).isEqualTo("LEGACY_SUPPLIER_SELECTION_MISSING");
        assertThat(restored.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(restored.supplierKey()).isNull();
        assertThat(restored.remoteProductId()).isNull();
        assertThat(restored.lastErrorMessage())
                .isEqualTo("Legacy paid order has no frozen supplier selection.");
    }
}
