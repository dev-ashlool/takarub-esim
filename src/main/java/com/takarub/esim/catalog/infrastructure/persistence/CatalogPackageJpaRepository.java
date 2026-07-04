package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.takarub.esim.supplier.domain.model.DataUnit;

public interface CatalogPackageJpaRepository extends JpaRepository<CatalogPackageEntity, String> {

    Optional<CatalogPackageEntity> findByCountry_IdAndDataAmountAndDataUnitAndDurationDays(
            String countryId, int dataAmount, DataUnit dataUnit, int durationDays);

    List<CatalogPackageEntity> findAll();

    List<CatalogPackageEntity> findByAvailableTrue();

    List<CatalogPackageEntity> findByCountry_IdAndAvailableTrue(String countryId);

    @Query("""
            SELECT p FROM CatalogPackageEntity p JOIN FETCH p.country c
            WHERE p.available = true
              AND (:term IS NULL
                OR LOWER(c.englishName) LIKE LOWER(:term)
                OR LOWER(c.arabicName) LIKE LOWER(:term)
                OR LOWER(c.id) LIKE LOWER(:term)
                OR LOWER(CAST(p.dataUnit AS string)) LIKE LOWER(:term)
                OR CAST(p.dataAmount AS string) LIKE :term
                OR CAST(p.durationDays AS string) LIKE :term)
              AND (:countryIso IS NULL OR c.id = :countryIso)
              AND (:dataAmount IS NULL OR p.dataAmount = :dataAmount)
              AND (:dataUnit IS NULL OR p.dataUnit = :dataUnit)
              AND (:durationDays IS NULL OR p.durationDays = :durationDays)
            """)
    Page<CatalogPackageEntity> searchAvailable(
            @Param("term") String term,
            @Param("countryIso") String countryIso,
            @Param("dataAmount") Integer dataAmount,
            @Param("dataUnit") DataUnit dataUnit,
            @Param("durationDays") Integer durationDays,
            Pageable pageable);
}
