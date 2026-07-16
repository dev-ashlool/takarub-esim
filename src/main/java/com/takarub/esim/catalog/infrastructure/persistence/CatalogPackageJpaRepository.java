package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.supplier.domain.model.DataUnit;

public interface CatalogPackageJpaRepository extends JpaRepository<CatalogPackageEntity, String> {

    Optional<CatalogPackageEntity> findByCountry_IdAndDataAmountAndDataUnitAndDurationDays(
            String countryId, int dataAmount, DataUnit dataUnit, int durationDays);

    List<CatalogPackageEntity> findAll();

    List<CatalogPackageEntity> findByAvailableTrue();

    List<CatalogPackageEntity> findByCountry_IdAndAvailableTrue(String countryId);
}
