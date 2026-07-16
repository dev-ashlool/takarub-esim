package com.takarub.esim.catalog.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.catalog.application.query.BrowseCatalogQuery;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;
import com.takarub.esim.catalog.presentation.mapper.CatalogMapper;
import com.takarub.esim.catalog.presentation.response.CatalogPackageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/catalog/packages")
@Tag(name = "Catalog", description = "Browse unified eSIM catalog packages")
public class CatalogController {

    private final BrowseCatalogUseCase browseCatalogUseCase;
    private final CatalogMapper catalogMapper;

    public CatalogController(BrowseCatalogUseCase browseCatalogUseCase, CatalogMapper catalogMapper) {
        this.browseCatalogUseCase = browseCatalogUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GetMapping
    @Operation(
            summary = "List available catalog packages",
            description = "Returns storefront-visible packages that are currently marked available. "
                    + "Optionally filter by ISO-3166 alpha-2 country code.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Available packages returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogPackageResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not authorized")
    })
    public ResponseEntity<List<CatalogPackageResponse>> listPackages(
            @Parameter(description = "Optional ISO-3166 alpha-2 country filter, e.g. JO")
            @RequestParam(name = "countryIso", required = false) String countryIso) {
        List<CatalogPackageResponse> packages = browseCatalogUseCase
                .execute(new BrowseCatalogQuery(countryIso))
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(packages);
    }
}
