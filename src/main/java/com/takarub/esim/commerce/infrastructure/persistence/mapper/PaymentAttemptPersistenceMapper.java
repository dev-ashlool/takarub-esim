package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.infrastructure.persistence.entity.PaymentAttemptJpaEntity;

/**
 * Translates between the {@link PaymentAttempt} aggregate and {@link PaymentAttemptJpaEntity}.
 * Entity → domain uses {@link PaymentAttempt#reconstitute}; no reflection and no {@code create()}.
 */
@Component
public class PaymentAttemptPersistenceMapper {

    public PaymentAttemptJpaEntity toEntity(PaymentAttempt attempt) {
        return new PaymentAttemptJpaEntity(
                attempt.id().value().toString(),
                attempt.orderId().value().toString(),
                attempt.status(),
                attempt.amount(),
                attempt.currency(),
                attempt.externalOrderId(),
                attempt.externalTransactionId(),
                attempt.createdAt(),
                attempt.updatedAt());
    }

    public PaymentAttempt toDomain(PaymentAttemptJpaEntity entity) {
        return PaymentAttempt.reconstitute(
                PaymentAttemptId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                OrderId.of(entity.getOrderId()),
                entity.getStatus(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getExternalOrderId(),
                entity.getExternalTransactionId());
    }
}
