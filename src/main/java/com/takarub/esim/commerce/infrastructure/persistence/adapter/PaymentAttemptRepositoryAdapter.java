package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.repository.PaymentAttemptJpaRepository;

/**
 * Outbound adapter implementing the {@link PaymentAttemptRepository} domain port over Spring Data
 * JPA. Pure translation and delegation; repository calls join the active transaction when present.
 */
@Component
public class PaymentAttemptRepositoryAdapter implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository paymentAttemptJpaRepository;
    private final PaymentAttemptPersistenceMapper mapper;

    public PaymentAttemptRepositoryAdapter(PaymentAttemptJpaRepository paymentAttemptJpaRepository,
                                           PaymentAttemptPersistenceMapper mapper) {
        this.paymentAttemptJpaRepository = paymentAttemptJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt paymentAttempt) {
        return mapper.toDomain(paymentAttemptJpaRepository.save(mapper.toEntity(paymentAttempt)));
    }

    @Override
    public Optional<PaymentAttempt> findById(PaymentAttemptId id) {
        return paymentAttemptJpaRepository.findById(id.value().toString()).map(mapper::toDomain);
    }

    @Override
    public List<PaymentAttempt> findByOrderId(OrderId orderId) {
        return paymentAttemptJpaRepository.findByOrderId(orderId.value().toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PaymentAttempt> findActiveInitiatedByOrderId(OrderId orderId) {
        return paymentAttemptJpaRepository
                .findByOrderIdAndStatus(orderId.value().toString(), PaymentAttemptStatus.INITIATED)
                .map(mapper::toDomain);
    }
}
