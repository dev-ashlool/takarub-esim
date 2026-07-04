package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.catalog.infrastructure.config.CacheConfig;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.CountryView;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.application.result.PagedResult;
import com.takarub.esim.supplier.domain.model.DataUnit;

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
    @Cacheable(value = CacheConfig.CATALOG_PACKAGES, key = "'list:' + (#countryIso != null ? #countryIso : 'ALL')")
    @Transactional(readOnly = true)
    public List<CatalogPackageView> findAvailablePackages(String countryIso) {
        List<CatalogPackageEntity> packages = countryIso == null
                ? catalogPackageJpaRepository.findByAvailableTrue()
                : catalogPackageJpaRepository.findByCountry_IdAndAvailableTrue(countryIso);

        return packages.stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_COUNTRIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<CountryView> findCountriesWithAvailablePackages() {
        List<CatalogPackageEntity> available = catalogPackageJpaRepository.findByAvailableTrue();

        Map<String, List<CatalogPackageEntity>> byCountry = available.stream()
                .collect(Collectors.groupingBy(p -> p.getCountry().getId()));

        return byCountry.entrySet().stream()
                .map(entry -> {
                    CountryEntity country = entry.getValue().get(0).getCountry();
                    return new CountryView(
                            country.getId(),
                            country.getArabicName(),
                            country.getEnglishName(),
                            country.getFlagImageUrl(),
                            entry.getValue().size(),
                            country.getLocationType());
                })
                .sorted((a, b) -> a.englishName().compareToIgnoreCase(b.englishName()))
                .toList();
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_SEARCH,
               key = "T(java.util.Objects).hash(#searchTerm, #countryIso, #dataAmount, #dataUnit, #durationDays, #page, #size)")
    @Transactional(readOnly = true)
    public PagedResult<CatalogPackageView> searchAvailablePackages(
            String searchTerm, String countryIso, Integer dataAmount,
            String dataUnit, Integer durationDays, int page, int size) {
        String likeTerm = searchTerm != null ? "%" + searchTerm + "%" : null;
        DataUnit dataUnitEnum = parseDataUnit(dataUnit);
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by("country.englishName").ascending()
                        .and(Sort.by("dataAmount").ascending()));

        Page<CatalogPackageEntity> resultPage = catalogPackageJpaRepository.searchAvailable(
                likeTerm, countryIso, dataAmount, dataUnitEnum, durationDays, pageRequest);

        List<CatalogPackageView> content = resultPage.getContent().stream()
                .map(this::toView)
                .toList();

        return new PagedResult<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages());
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_PACKAGE_DETAILS, key = "#packageId")
    @Transactional(readOnly = true)
    public Optional<PackageDetailsView> findPackageById(String packageId) {
        return catalogPackageJpaRepository.findById(packageId)
                .map(this::toDetailsView);
    }

    private PackageDetailsView toDetailsView(CatalogPackageEntity entity) {
        CountryEntity country = entity.getCountry();
        return new PackageDetailsView(
                entity.getId(),
                country.getId(),
                country.getArabicName(),
                country.getEnglishName(),
                country.getFlagImageUrl(),
                entity.getDataAmount(),
                entity.getDataUnit(),
                entity.getDurationDays(),
                entity.isAvailable(),
                entity.getLocationType());
    }

    private static DataUnit parseDataUnit(String dataUnit) {
        if (dataUnit == null || dataUnit.isBlank()) {
            return null;
        }
        try {
            return DataUnit.valueOf(dataUnit.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
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
                entity.getDurationDays(),
                entity.getLocationType());
    }
}
