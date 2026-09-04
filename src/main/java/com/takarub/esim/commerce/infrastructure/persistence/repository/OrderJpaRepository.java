package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderJpaEntity;

/**
 * Spring Data JPA repository for {@link OrderJpaEntity}. Item collections are fetched via entity
 * graphs so read paths work with {@code open-in-view: false}.
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    @EntityGraph(attributePaths = "items")
    @Override
    Optional<OrderJpaEntity> findById(String id);

    @EntityGraph(attributePaths = "items")
    Optional<OrderJpaEntity> findByCartId(String cartId);

    @EntityGraph(attributePaths = "items")
    Optional<OrderJpaEntity> findByUserIdAndCheckoutRequestId(String userId, String checkoutRequestId);
}
