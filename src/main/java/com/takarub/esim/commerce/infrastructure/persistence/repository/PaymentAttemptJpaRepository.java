package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.PaymentAttemptJpaEntity;

/**
 * Spring Data JPA repository for {@link PaymentAttemptJpaEntity}.
 */
public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptJpaEntity, String> {

    List<PaymentAttemptJpaEntity> findByOrderId(String orderId);

    Optional<PaymentAttemptJpaEntity> findByOrderIdAndStatus(String orderId, PaymentAttemptStatus status);
}
