package com.takarub.esim.catalog.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.catalog.application.command.UpdateExchangeRateCommand;
import com.takarub.esim.catalog.application.result.UpdateExchangeRateResult;
import com.takarub.esim.catalog.application.usecase.UpdateExchangeRateUseCase;
import com.takarub.esim.catalog.presentation.request.UpdateExchangeRateRequest;
import com.takarub.esim.catalog.presentation.response.UpdateExchangeRateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Admin endpoints for maintaining FX rates used by supplier cost normalization.
 */
@RestController
@RequestMapping("/api/v1/admin/exchange-rates")
@Tag(name = "Exchange Rate Administration", description = "Update FX rates and recalculate normalized supplier costs")
public class AdminExchangeRateController {

    private final UpdateExchangeRateUseCase updateExchangeRateUseCase;

    public AdminExchangeRateController(UpdateExchangeRateUseCase updateExchangeRateUseCase) {
        this.updateExchangeRateUseCase = updateExchangeRateUseCase;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update exchange rate",
            description = "Upserts a currency pair rate (typically XXX -> USD), then recalculates "
                    + "normalized_cost_price for supplier mappings whose original cost_currency matches "
                    + "baseCurrency and is not USD. Original cost fields are never modified. "
                    + "Does not re-sync supplier catalogs.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Rate saved and affected mappings recalculated",
                    content = @Content(schema = @Schema(implementation = UpdateExchangeRateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<UpdateExchangeRateResponse> updateExchangeRate(
            @Valid @RequestBody UpdateExchangeRateRequest request) {
        UpdateExchangeRateResult result = updateExchangeRateUseCase.execute(
                new UpdateExchangeRateCommand(
                        request.baseCurrency(),
                        request.targetCurrency(),
                        request.rate()));

        return ResponseEntity.ok(new UpdateExchangeRateResponse(
                result.baseCurrency(),
                result.targetCurrency(),
                result.rate(),
                result.mappingsRecalculated()));
    }
}
