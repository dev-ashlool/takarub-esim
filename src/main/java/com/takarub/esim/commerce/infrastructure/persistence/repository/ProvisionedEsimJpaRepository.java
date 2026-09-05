package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.commerce.infrastructure.persistence.entity.ProvisionedEsimJpaEntity;

public interface ProvisionedEsimJpaRepository extends JpaRepository<ProvisionedEsimJpaEntity, String> {

    Optional<ProvisionedEsimJpaEntity> findByOrderId(String orderId);

    Optional<ProvisionedEsimJpaEntity> findByFulfillmentWorkId(String fulfillmentWorkId);
}
