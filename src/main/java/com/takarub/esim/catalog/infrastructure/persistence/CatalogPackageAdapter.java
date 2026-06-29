package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.application.port.CatalogPackagePort;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Catalog package fingerprint resolution and presence-based availability updates.
 */
@Component
public class CatalogPackageAdapter implements CatalogPackagePort {

    private final CatalogPackageJpaRepository catalogPackageJpaRepository;
    private final CountryJpaRepository countryJpaRepository;

    public CatalogPackageAdapter(CatalogPackageJpaRepository catalogPackageJpaRepository,
                                 CountryJpaRepository countryJpaRepository) {
        this.catalogPackageJpaRepository = catalogPackageJpaRepository;
        this.countryJpaRepository = countryJpaRepository;
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
