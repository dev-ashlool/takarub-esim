package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;

public interface FulfillmentWorkJpaRepository extends JpaRepository<FulfillmentWorkJpaEntity, String> {

    Optional<FulfillmentWorkJpaEntity> findByOrderId(String orderId);
}
