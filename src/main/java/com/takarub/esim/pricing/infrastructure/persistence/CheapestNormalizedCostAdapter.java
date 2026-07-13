package com.takarub.esim.pricing.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.pricing.domain.port.CheapestNormalizedCostPort;
import com.takarub.esim.supplier.infrastructure.persistence.SupplierPackageMappingJpaRepository;

@Component
public class CheapestNormalizedCostAdapter implements CheapestNormalizedCostPort {

    private final SupplierPackageMappingJpaRepository mappingRepository;

    public CheapestNormalizedCostAdapter(SupplierPackageMappingJpaRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    @Override
    public Optional<BigDecimal> findCheapestInStockNormalizedCostUsd(String catalogPackageId) {
        return mappingRepository.findMinNormalizedCostForInStockPackage(catalogPackageId);
    }
}
