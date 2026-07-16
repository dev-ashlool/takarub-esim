package com.takarub.esim.catalog.presentation.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.presentation.response.CatalogPackageResponse;

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
                view.durationDays());
    }
}
