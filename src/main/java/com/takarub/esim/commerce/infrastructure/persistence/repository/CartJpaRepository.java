package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.CartJpaEntity;

/**
 * Spring Data JPA repository for {@link CartJpaEntity}. Item collections are fetched via entity
 * graphs so read paths work with {@code open-in-view: false}.
 */
public interface CartJpaRepository extends JpaRepository<CartJpaEntity, String> {

    @EntityGraph(attributePaths = "items")
    @Override
    Optional<CartJpaEntity> findById(String id);

    @EntityGraph(attributePaths = "items")
    Optional<CartJpaEntity> findByUserIdAndStatus(String userId, CartStatus status);
}
