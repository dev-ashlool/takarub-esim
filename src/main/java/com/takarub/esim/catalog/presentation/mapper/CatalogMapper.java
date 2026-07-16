package com.takarub.esim.catalog.presentation.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.CountryView;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.application.result.PagedResult;
import com.takarub.esim.catalog.presentation.response.CatalogPackageResponse;
import com.takarub.esim.catalog.presentation.response.CountryResponse;
import com.takarub.esim.catalog.presentation.response.PackageDetailsResponse;
import com.takarub.esim.catalog.presentation.response.SearchPackagesResponse;

@Component
public class CatalogMapper {

    public CatalogPackageResponse toResponse(CatalogPackageView view) {
        return new CatalogPackageResponse(
                view.id(),
                view.countryIso(),
                view.countryArabicName(),
                view.countryEnglishName(),
                view.flagImageUrl(),
                view.dataAmount(),
                view.dataUnit().name(),
                view.durationDays(),
                view.locationType().name());
    }

    public CountryResponse toResponse(CountryView view) {
        return new CountryResponse(
                view.iso(),
                view.arabicName(),
                view.englishName(),
                view.flagImageUrl(),
                view.packageCount(),
                view.locationType().name());
    }

    public PackageDetailsResponse toResponse(PackageDetailsView view) {
        return new PackageDetailsResponse(
                view.id(),
                view.countryIso(),
                view.countryArabicName(),
                view.countryEnglishName(),
                view.flagImageUrl(),
                view.dataAmount(),
                view.dataUnit().name(),
                view.durationDays(),
                view.available(),
                view.locationType().name());
    }

    public SearchPackagesResponse toSearchResponse(PagedResult<CatalogPackageView> result) {
        return new SearchPackagesResponse(
                result.content().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
