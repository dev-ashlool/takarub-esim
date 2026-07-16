package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.CatalogPackageView;

/**
 * JPA implementation of the catalog browse read port.
 */
@Component
public class CatalogBrowseAdapter implements CatalogBrowsePort {

    private final CatalogPackageJpaRepository catalogPackageJpaRepository;

    public CatalogBrowseAdapter(CatalogPackageJpaRepository catalogPackageJpaRepository) {
        this.catalogPackageJpaRepository = catalogPackageJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogPackageView> findAvailablePackages(String countryIso) {
        List<CatalogPackageEntity> packages = countryIso == null
                ? catalogPackageJpaRepository.findByAvailableTrue()
                : catalogPackageJpaRepository.findByCountry_IdAndAvailableTrue(countryIso);

        return packages.stream()
                .map(this::toView)
                .toList();
    }

    private CatalogPackageView toView(CatalogPackageEntity entity) {
        CountryEntity country = entity.getCountry();
        return new CatalogPackageView(
                entity.getId(),
                country.getId(),
                country.getArabicName(),
                country.getEnglishName(),
                country.getFlagImageUrl(),
                entity.getDataAmount(),
                entity.getDataUnit(),
                entity.getDurationDays());
    }
}
