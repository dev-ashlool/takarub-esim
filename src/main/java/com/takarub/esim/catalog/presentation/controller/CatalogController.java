package com.takarub.esim.catalog.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.catalog.application.query.BrowseCatalogQuery;
import com.takarub.esim.catalog.application.query.GetPackageDetailsQuery;
import com.takarub.esim.catalog.application.query.SearchPackagesQuery;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;
import com.takarub.esim.catalog.application.usecase.BrowseCountriesUseCase;
import com.takarub.esim.catalog.application.usecase.PackageDetailsUseCase;
import com.takarub.esim.catalog.application.usecase.SearchPackagesUseCase;
import com.takarub.esim.catalog.presentation.mapper.CatalogMapper;
import com.takarub.esim.catalog.presentation.response.CatalogPackageResponse;
import com.takarub.esim.catalog.presentation.response.CountryResponse;
import com.takarub.esim.catalog.presentation.response.PackageDetailsResponse;
import com.takarub.esim.catalog.presentation.response.SearchPackagesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "Browse unified eSIM catalog — countries and packages")
public class CatalogController {

    private final BrowseCatalogUseCase browseCatalogUseCase;
    private final BrowseCountriesUseCase browseCountriesUseCase;
    private final PackageDetailsUseCase packageDetailsUseCase;
    private final SearchPackagesUseCase searchPackagesUseCase;
    private final CatalogMapper catalogMapper;

    public CatalogController(BrowseCatalogUseCase browseCatalogUseCase,
                             BrowseCountriesUseCase browseCountriesUseCase,
                             PackageDetailsUseCase packageDetailsUseCase,
                             SearchPackagesUseCase searchPackagesUseCase,
                             CatalogMapper catalogMapper) {
        this.browseCatalogUseCase = browseCatalogUseCase;
        this.browseCountriesUseCase = browseCountriesUseCase;
        this.packageDetailsUseCase = packageDetailsUseCase;
        this.searchPackagesUseCase = searchPackagesUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GetMapping("/countries")
    @Operation(
            summary = "List countries with available packages",
            description = "Returns countries that have at least one available eSIM package, "
                    + "sorted alphabetically. No authentication required.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Countries returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CountryResponse.class))))
    })
    public ResponseEntity<List<CountryResponse>> listCountries() {
        List<CountryResponse> countries = browseCountriesUseCase.execute()
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/packages/search")
    @Operation(
            summary = "Search catalog packages",
            description = "Searches available packages by country name (Arabic or English), "
                    + "data unit, data amount, or duration. Returns paginated results sorted by "
                    + "country name. No authentication required.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results returned",
                    content = @Content(schema = @Schema(implementation = SearchPackagesResponse.class)))
    })
    public ResponseEntity<SearchPackagesResponse> searchPackages(
            @Parameter(description = "Search term (e.g. country name, GB, duration)")
            @RequestParam(name = "q", required = false) String q,
            @Parameter(description = "Filter by country ISO code, e.g. JO")
            @RequestParam(name = "countryIso", required = false) String countryIso,
            @Parameter(description = "Filter by data amount, e.g. 3")
            @RequestParam(name = "dataAmount", required = false) Integer dataAmount,
            @Parameter(description = "Filter by data unit, e.g. GB")
            @RequestParam(name = "dataUnit", required = false) String dataUnit,
            @Parameter(description = "Filter by duration in days, e.g. 30")
            @RequestParam(name = "durationDays", required = false) Integer durationDays,
            @Parameter(description = "Page number (0-based, default 0)")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(name = "size", defaultValue = "20") int size) {
        SearchPackagesResponse response = catalogMapper.toSearchResponse(
                searchPackagesUseCase.execute(new SearchPackagesQuery(q, countryIso, dataAmount, dataUnit, durationDays, page, size)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/packages/{id}")
    @Operation(
            summary = "Get package details",
            description = "Returns full details for a single catalog package by its identifier. "
                    + "No authentication required.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Package details returned",
                    content = @Content(schema = @Schema(implementation = PackageDetailsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<PackageDetailsResponse> getPackageDetails(
            @Parameter(description = "Catalog package identifier (UUID)")
            @PathVariable String id) {
        PackageDetailsResponse response = catalogMapper.toResponse(
                packageDetailsUseCase.execute(new GetPackageDetailsQuery(id)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/packages")
    @Operation(
            summary = "List available catalog packages",
            description = "Returns storefront-visible packages that are currently marked available. "
                    + "Optionally filter by ISO-3166 alpha-2 country code. No authentication required.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Available packages returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogPackageResponse.class))))
    })
    public ResponseEntity<List<CatalogPackageResponse>> listPackages(
            @Parameter(description = "SEO country slug filter, e.g. jordan (takes precedence over countryIso)")
            @RequestParam(name = "country", required = false) String country,
            @Parameter(description = "Optional ISO-3166 / location id filter, e.g. JO (legacy)")
            @RequestParam(name = "countryIso", required = false) String countryIso) {
        List<CatalogPackageResponse> packages = browseCatalogUseCase
                .execute(new BrowseCatalogQuery(countryIso, country))
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(packages);
    }
}
