package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.application.port.CatalogPackagePort;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Catalog package fingerprint resolution and presence-based availability updates.
 */
@Component
public class CatalogPackageAdapter implements CatalogPackagePort {

    private static final Logger log = LoggerFactory.getLogger(CatalogPackageAdapter.class);

    private final CatalogPackageJpaRepository catalogPackageJpaRepository;
    private final CountryJpaRepository countryJpaRepository;

    public CatalogPackageAdapter(CatalogPackageJpaRepository catalogPackageJpaRepository,
                                 CountryJpaRepository countryJpaRepository) {
        this.catalogPackageJpaRepository = catalogPackageJpaRepository;
        this.countryJpaRepository = countryJpaRepository;
    }

    @Override
    public void ensureCountry(String iso, String englishName, String flagImageUrl) {
        CountryEntity entity = countryJpaRepository.findById(iso).orElse(null);
        if (entity == null) {
            String name = (englishName != null && !englishName.isBlank()) ? englishName : iso;
            countryJpaRepository.save(new CountryEntity(iso, name, name, flagImageUrl));
        } else {
            boolean updated = false;
            if (englishName != null && !englishName.isBlank() && !englishName.equals(entity.getEnglishName())) {
                entity.setEnglishName(englishName);
                entity.setArabicName(englishName);
                updated = true;
            }
            if (flagImageUrl != null && !flagImageUrl.isBlank() && !flagImageUrl.equals(entity.getFlagImageUrl())) {
                entity.setFlagImageUrl(flagImageUrl);
                updated = true;
            }
            if (updated) {
                countryJpaRepository.save(entity);
            }
        }
    }

    @Override
    public void ensureLocation(String locationId, String displayName, String flagImageUrl, LocationType locationType) {
        CountryEntity entity = countryJpaRepository.findById(locationId).orElse(null);
        if (entity == null) {
            String name = (displayName != null && !displayName.isBlank()) ? displayName : locationId;
            countryJpaRepository.save(new CountryEntity(locationId, name, name, flagImageUrl, locationType));
        } else {
            boolean updated = false;
            if (displayName != null && !displayName.isBlank() && !displayName.equals(entity.getEnglishName())) {
                entity.setEnglishName(displayName);
                entity.setArabicName(displayName);
                updated = true;
            }
            if (flagImageUrl != null && !flagImageUrl.isBlank() && !flagImageUrl.equals(entity.getFlagImageUrl())) {
                entity.setFlagImageUrl(flagImageUrl);
                updated = true;
            }
            if (entity.getLocationType() != locationType) {
                entity.setLocationType(locationType);
                updated = true;
            }
            if (updated) {
                countryJpaRepository.save(entity);
            }
        }
    }

    @Override
    public String resolvePackageId(String countryIso, int dataAmount, DataUnit dataUnit, int durationDays) {
        return catalogPackageJpaRepository
                .findByCountry_IdAndDataAmountAndDataUnitAndDurationDays(
                        countryIso, dataAmount, dataUnit, durationDays)
                .map(CatalogPackageEntity::getId)
                .orElseGet(() -> createPackage(countryIso, dataAmount, dataUnit, durationDays));
    }

    @Override
    public void markAvailable(Set<String> catalogPackageIds) {
        for (String packageId : catalogPackageIds) {
            CatalogPackageEntity entity = catalogPackageJpaRepository.findById(packageId)
                    .orElseThrow(() -> new PackageNotFoundException("Catalog package not found: " + packageId));
            entity.setAvailable(true);
            catalogPackageJpaRepository.save(entity);
        }
    }

    @Override
    public int markUnavailableExcept(Set<String> availableCatalogPackageIds) {
        Set<String> available = new HashSet<>(availableCatalogPackageIds);
        int marked = 0;
        List<CatalogPackageEntity> allPackages = catalogPackageJpaRepository.findAll();
        for (CatalogPackageEntity catalogPackage : allPackages) {
            if (!available.contains(catalogPackage.getId()) && catalogPackage.isAvailable()) {
                catalogPackage.setAvailable(false);
                catalogPackageJpaRepository.save(catalogPackage);
                marked++;
            }
        }
        return marked;
    }

    private String createPackage(String countryIso, int dataAmount, DataUnit dataUnit, int durationDays) {
        CountryEntity country = requireCountry(countryIso);

        String packageId = UUID.randomUUID().toString();
        log.info("Creating catalog package id={} (len={}) country={} (len={}) data={} {} days={}",
                packageId, packageId.length(), countryIso, countryIso.length(),
                dataAmount, dataUnit, durationDays);
        CatalogPackageEntity created = new CatalogPackageEntity(
                packageId, country, dataAmount, dataUnit, durationDays, true);
        catalogPackageJpaRepository.save(created);
        return packageId;
    }

    private CountryEntity requireCountry(String countryIso) {
        return countryJpaRepository.findById(countryIso)
                .orElseGet(() -> countryJpaRepository.save(createPlaceholderCountry(countryIso)));
    }

    private static CountryEntity createPlaceholderCountry(String countryIso) {
        return new CountryEntity(countryIso, countryIso, countryIso, null);
    }
}
