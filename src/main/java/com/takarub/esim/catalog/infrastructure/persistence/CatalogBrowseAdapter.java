package com.takarub.esim.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.CountryView;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.application.result.PagedResult;
import com.takarub.esim.catalog.infrastructure.config.CacheConfig;
import com.takarub.esim.pricing.domain.model.SellPrice;
import com.takarub.esim.pricing.domain.service.SellPriceResolver;
import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * JPA implementation of the catalog browse read port.
 * Only packages with a resolvable sell price are exposed publicly.
 */
@Component
public class CatalogBrowseAdapter implements CatalogBrowsePort {

    private final CatalogPackageJpaRepository catalogPackageJpaRepository;
    private final CountryJpaRepository countryJpaRepository;
    private final SellPriceResolver sellPriceResolver;

    public CatalogBrowseAdapter(CatalogPackageJpaRepository catalogPackageJpaRepository,
                                CountryJpaRepository countryJpaRepository,
                                SellPriceResolver sellPriceResolver) {
        this.catalogPackageJpaRepository = catalogPackageJpaRepository;
        this.countryJpaRepository = countryJpaRepository;
        this.sellPriceResolver = sellPriceResolver;
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_PACKAGES, key = "'list:' + (#countryIso != null ? #countryIso : 'ALL')")
    @Transactional(readOnly = true)
    public List<CatalogPackageView> findAvailablePackages(String countryIso) {
        List<CatalogPackageEntity> packages = countryIso == null
                ? catalogPackageJpaRepository.findByAvailableTrue()
                : catalogPackageJpaRepository.findByCountry_IdAndAvailableTrue(countryIso);

        return packages.stream()
                .map(this::toSellableView)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_COUNTRIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<CountryView> findCountriesWithAvailablePackages() {
        List<CatalogPackageView> sellable = catalogPackageJpaRepository.findByAvailableTrue().stream()
                .map(this::toSellableView)
                .flatMap(Optional::stream)
                .toList();

        Map<String, List<CatalogPackageView>> byCountry = sellable.stream()
                .collect(Collectors.groupingBy(CatalogPackageView::countryIso));

        return byCountry.entrySet().stream()
                .map(entry -> {
                    List<CatalogPackageView> packages = entry.getValue();
                    CatalogPackageView sample = packages.get(0);
                    BigDecimal minimumPrice = packages.stream()
                            .map(CatalogPackageView::price)
                            .min(Comparator.naturalOrder())
                            .orElseThrow();
                    return new CountryView(
                            sample.countryIso(),
                            sample.countryArabicName(),
                            sample.countryEnglishName(),
                            sample.flagImageUrl(),
                            packages.size(),
                            sample.locationType(),
                            sample.countrySlug(),
                            minimumPrice);
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
        Sort sort = Sort.by("country.englishName").ascending()
                .and(Sort.by("dataAmount").ascending());

        List<CatalogPackageView> sellable = catalogPackageJpaRepository.searchAvailable(
                        likeTerm, countryIso, dataAmount, dataUnitEnum, durationDays, Pageable.unpaged(sort))
                .getContent()
                .stream()
                .map(this::toSellableView)
                .flatMap(Optional::stream)
                .toList();

        int from = Math.min(page * size, sellable.size());
        int to = Math.min(from + size, sellable.size());
        List<CatalogPackageView> content = new ArrayList<>(sellable.subList(from, to));
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(sellable.size() / (double) size);

        return new PagedResult<>(content, page, size, sellable.size(), totalPages);
    }

    @Override
    @Cacheable(value = CacheConfig.CATALOG_PACKAGE_DETAILS, key = "#packageId")
    @Transactional(readOnly = true)
    public Optional<PackageDetailsView> findPackageById(String packageId) {
        return catalogPackageJpaRepository.findById(packageId)
                .flatMap(this::toSellableDetailsView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findCountryIdBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return countryJpaRepository.findBySlugIgnoreCase(slug.trim())
                .map(CountryEntity::getId);
    }

    private Optional<CatalogPackageView> toSellableView(CatalogPackageEntity entity) {
        Optional<SellPrice> sellPrice = sellPriceResolver.resolve(entity.getId());
        if (sellPrice.isEmpty()) {
            return Optional.empty();
        }
        CountryEntity country = entity.getCountry();
        SellPrice price = sellPrice.get();
        return Optional.of(new CatalogPackageView(
                entity.getId(),
                country.getId(),
                country.getArabicName(),
                country.getEnglishName(),
                country.getFlagImageUrl(),
                entity.getDataAmount(),
                entity.getDataUnit(),
                entity.getDurationDays(),
                entity.getLocationType(),
                price.amount(),
                price.currency(),
                country.getSlug()));
    }

    private Optional<PackageDetailsView> toSellableDetailsView(CatalogPackageEntity entity) {
        Optional<SellPrice> sellPrice = sellPriceResolver.resolve(entity.getId());
        if (sellPrice.isEmpty()) {
            return Optional.empty();
        }
        CountryEntity country = entity.getCountry();
        SellPrice price = sellPrice.get();
        return Optional.of(new PackageDetailsView(
                entity.getId(),
                country.getId(),
                country.getArabicName(),
                country.getEnglishName(),
                country.getFlagImageUrl(),
                entity.getDataAmount(),
                entity.getDataUnit(),
                entity.getDurationDays(),
                entity.isAvailable(),
                entity.getLocationType(),
                price.amount(),
                price.currency(),
                country.getSlug()));
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
}
