package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierLikeCardProductJpaRepository extends JpaRepository<SupplierLikeCardProductEntity, Integer> {

    Optional<SupplierLikeCardProductEntity> findByRemoteProductId(String remoteProductId);
}
