package com.takarub.esim.commerce.domain.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class FulfillmentWorkTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final Instant CLAIMED = Instant.parse("2026-09-05T12:05:00Z");
    private static final Instant LATER = Instant.parse("2026-09-05T12:10:00Z");

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final SystemClockProvider clock =
            new SystemClockProvider(Clock.fixed(NOW, ZoneOffset.UTC));
    private final SystemClockProvider laterClock =
            new SystemClockProvider(Clock.fixed(LATER, ZoneOffset.UTC));

    @Test
    void pendingRequiresSupplierSourcing() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        FulfillmentWork work = FulfillmentWork.pending(
                idGenerator, clock, orderId, "LIKE_CARD", "5653");

        assertThat(work.status()).isEqualTo(FulfillmentStatus.PENDING);
        assertThat(work.orderId()).isEqualTo(orderId);
        assertThat(work.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(work.remoteProductId()).isEqualTo("5653");
        assertThat(work.lastErrorCode()).isNull();
        assertThat(work.lastErrorMessage()).isNull();
        assertThat(work.claimedAt()).isNull();
        assertThat(work.createdAt()).isEqualTo(NOW);
        assertThat(work.updatedAt()).isEqualTo(NOW);
        assertThat(work.id()).isNotNull();
    }

    @Test
    void pendingRejectsMissingSupplierKey() {
        assertThatThrownBy(() -> FulfillmentWork.pending(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), null, "5653"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierKey");
    }

    @Test
    void blockedAllowsNullSourcingWithErrorReason() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        FulfillmentWork work = FulfillmentWork.blocked(
                idGenerator,
                clock,
                orderId,
                null,
                null,
                "LEGACY_SUPPLIER_SELECTION_MISSING",
                "Legacy paid order has no frozen supplier selection.");

        assertThat(work.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(work.supplierKey()).isNull();
        assertThat(work.remoteProductId()).isNull();
        assertThat(work.lastErrorCode()).isEqualTo("LEGACY_SUPPLIER_SELECTION_MISSING");
        assertThat(work.lastErrorMessage())
                .isEqualTo("Legacy paid order has no frozen supplier selection.");
    }

    @Test
    void blockedRequiresErrorReason() {
        assertThatThrownBy(() -> FulfillmentWork.blocked(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), null, null, " ", "msg"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorCode");
    }

    @Test
    void reconstitutesPendingAndBlocked() {
        FulfillmentId id = FulfillmentId.of(UUID.randomUUID());
        OrderId orderId = OrderId.of(UUID.randomUUID());

        FulfillmentWork pending = FulfillmentWork.reconstitute(
                id,
                orderId,
                "LIKE_CARD",
                "100",
                FulfillmentStatus.PENDING,
                null,
                null,
                null,
                NOW,
                NOW);
        assertThat(pending.id()).isEqualTo(id);
        assertThat(pending.status()).isEqualTo(FulfillmentStatus.PENDING);

        FulfillmentWork blocked = FulfillmentWork.reconstitute(
                id,
                orderId,
                null,
                null,
                FulfillmentStatus.BLOCKED,
                null,
                "LEGACY_SUPPLIER_SELECTION_MISSING",
                "Legacy paid order has no frozen supplier selection.",
                NOW,
                NOW);
        assertThat(blocked.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(blocked.lastErrorCode()).isEqualTo("LEGACY_SUPPLIER_SELECTION_MISSING");
    }

    @Test
    void reconstitutePendingRejectsMissingSupplierKey() {
        assertThatThrownBy(() -> reconstitute(
                null,
                "100",
                FulfillmentStatus.PENDING,
                null,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierKey");
    }

    @Test
    void reconstitutePendingRejectsNonNullClaimedAt() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.PENDING,
                CLAIMED,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("claimedAt");
    }

    @Test
    void reconstitutePendingRejectsUnexpectedErrorReason() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.PENDING,
                null,
                "SOME_CODE",
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorCode");
    }

    @Test
    void reconstituteProcessingRequiresSupplierSourcing() {
        assertThatThrownBy(() -> reconstitute(
                null,
                "100",
                FulfillmentStatus.PROCESSING,
                CLAIMED,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierKey");
    }

    @Test
    void reconstituteProcessingRequiresClaimedAt() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.PROCESSING,
                null,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("claimedAt");
    }

    @Test
    void reconstituteFulfilledRequiresSupplierSourcing() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                null,
                FulfillmentStatus.FULFILLED,
                CLAIMED,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("remoteProductId");
    }

    @Test
    void reconstituteFulfilledRequiresClaimedAt() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.FULFILLED,
                null,
                null,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("claimedAt");
    }

    @Test
    void reconstituteFulfilledRejectsErrorFields() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.FULFILLED,
                CLAIMED,
                "X",
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorCode");
    }

    @Test
    void reconstituteUnknownRequiresSupplierSourcing() {
        assertThatThrownBy(() -> reconstitute(
                null,
                null,
                FulfillmentStatus.UNKNOWN,
                CLAIMED,
                "TIMEOUT",
                "Ambiguous supplier outcome"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("supplierKey");
    }

    @Test
    void reconstituteUnknownRequiresClaimedAt() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.UNKNOWN,
                null,
                "TIMEOUT",
                "Ambiguous supplier outcome"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("claimedAt");
    }

    @Test
    void reconstituteUnknownRequiresErrorCodeAndMessage() {
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.UNKNOWN,
                CLAIMED,
                null,
                "Ambiguous supplier outcome"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorCode");
        assertThatThrownBy(() -> reconstitute(
                "LIKE_CARD",
                "100",
                FulfillmentStatus.UNKNOWN,
                CLAIMED,
                "TIMEOUT",
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorMessage");
    }

    @Test
    void reconstituteBlockedWithoutSupplierSourcingRemainsValid() {
        FulfillmentWork blocked = reconstitute(
                null,
                null,
                FulfillmentStatus.BLOCKED,
                null,
                "LEGACY_SUPPLIER_SELECTION_MISSING",
                "Legacy paid order has no frozen supplier selection.");
        assertThat(blocked.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(blocked.supplierKey()).isNull();
        assertThat(blocked.remoteProductId()).isNull();
    }

    @Test
    void markFulfilledFromProcessing() {
        FulfillmentWork work = processingWork();
        Instant claimedAt = work.claimedAt();

        work.markFulfilled(laterClock);

        assertThat(work.status()).isEqualTo(FulfillmentStatus.FULFILLED);
        assertThat(work.claimedAt()).isEqualTo(claimedAt);
        assertThat(work.lastErrorCode()).isNull();
        assertThat(work.lastErrorMessage()).isNull();
        assertThat(work.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void markUnknownFromProcessing() {
        FulfillmentWork work = processingWork();
        Instant claimedAt = work.claimedAt();

        work.markUnknown(laterClock, "TIMEOUT", "Ambiguous outcome");

        assertThat(work.status()).isEqualTo(FulfillmentStatus.UNKNOWN);
        assertThat(work.claimedAt()).isEqualTo(claimedAt);
        assertThat(work.lastErrorCode()).isEqualTo("TIMEOUT");
        assertThat(work.lastErrorMessage()).isEqualTo("Ambiguous outcome");
        assertThat(work.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void markBlockedFromProcessing() {
        FulfillmentWork work = processingWork();
        Instant claimedAt = work.claimedAt();

        work.markBlocked(laterClock, "SUPPLIER_REJECTED", "Rejected");

        assertThat(work.status()).isEqualTo(FulfillmentStatus.BLOCKED);
        assertThat(work.claimedAt()).isEqualTo(claimedAt);
        assertThat(work.lastErrorCode()).isEqualTo("SUPPLIER_REJECTED");
        assertThat(work.lastErrorMessage()).isEqualTo("Rejected");
        assertThat(work.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void markTransitionsRejectNonProcessing() {
        FulfillmentWork pending = FulfillmentWork.pending(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), "LIKE_CARD", "5653");

        assertThatThrownBy(() -> pending.markFulfilled(laterClock))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PROCESSING");
        assertThatThrownBy(() -> pending.markUnknown(laterClock, "X", "Y"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> pending.markBlocked(laterClock, "X", "Y"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void markUnknownAndBlockedRejectBlankErrors() {
        FulfillmentWork work = processingWork();

        assertThatThrownBy(() -> work.markUnknown(laterClock, " ", "msg"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorCode");
        assertThatThrownBy(() -> work.markBlocked(laterClock, "CODE", " "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lastErrorMessage");
    }

    private FulfillmentWork processingWork() {
        return FulfillmentWork.reconstitute(
                FulfillmentId.of(UUID.randomUUID()),
                OrderId.of(UUID.randomUUID()),
                "LIKE_CARD",
                "5653",
                FulfillmentStatus.PROCESSING,
                CLAIMED,
                null,
                null,
                NOW,
                NOW);
    }

    private static FulfillmentWork reconstitute(
            String supplierKey,
            String remoteProductId,
            FulfillmentStatus status,
            Instant claimedAt,
            String lastErrorCode,
            String lastErrorMessage) {
        return FulfillmentWork.reconstitute(
                FulfillmentId.of(UUID.randomUUID()),
                OrderId.of(UUID.randomUUID()),
                supplierKey,
                remoteProductId,
                status,
                claimedAt,
                lastErrorCode,
                lastErrorMessage,
                NOW,
                NOW);
    }
}
