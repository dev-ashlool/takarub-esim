package com.takarub.esim.pricing.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.pricing.application.command.UpsertGlobalMarkupCommand;
import com.takarub.esim.pricing.application.command.UpsertPackagePricingCommand;
import com.takarub.esim.pricing.application.result.PricingConfigView;
import com.takarub.esim.pricing.application.usecase.DeletePackagePricingUseCase;
import com.takarub.esim.pricing.application.usecase.GetPricingConfigUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertGlobalMarkupUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertPackagePricingUseCase;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.presentation.request.UpsertGlobalMarkupRequest;
import com.takarub.esim.pricing.presentation.request.UpsertPackagePricingRequest;
import com.takarub.esim.pricing.presentation.response.PricingConfigResponse;
import com.takarub.esim.pricing.presentation.response.PricingRuleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/pricing")
@Tag(name = "Pricing Administration", description = "Configure global markup and per-package sell-price rules")
public class AdminPricingController {

    private final UpsertGlobalMarkupUseCase upsertGlobalMarkupUseCase;
    private final UpsertPackagePricingUseCase upsertPackagePricingUseCase;
    private final DeletePackagePricingUseCase deletePackagePricingUseCase;
    private final GetPricingConfigUseCase getPricingConfigUseCase;

    public AdminPricingController(UpsertGlobalMarkupUseCase upsertGlobalMarkupUseCase,
                                  UpsertPackagePricingUseCase upsertPackagePricingUseCase,
                                  DeletePackagePricingUseCase deletePackagePricingUseCase,
                                  GetPricingConfigUseCase getPricingConfigUseCase) {
        this.upsertGlobalMarkupUseCase = upsertGlobalMarkupUseCase;
        this.upsertPackagePricingUseCase = upsertPackagePricingUseCase;
        this.deletePackagePricingUseCase = deletePackagePricingUseCase;
        this.getPricingConfigUseCase = getPricingConfigUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get current pricing configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pricing configuration returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<PricingConfigResponse> getPricingConfig() {
        PricingConfigView view = getPricingConfigUseCase.execute();
        PricingConfigResponse.GlobalMarkupResponse global = view.globalMarkup() == null
                ? null
                : new PricingConfigResponse.GlobalMarkupResponse(
                        view.globalMarkup().percentage(), view.globalMarkup().updatedAt());
        var packageRules = view.packageRules().stream()
                .map(rule -> new PricingConfigResponse.PackagePricingResponse(
                        rule.catalogPackageId(),
                        rule.type(),
                        rule.percentage(),
                        rule.fixedPrice(),
                        rule.updatedAt()))
                .toList();
        return ResponseEntity.ok(new PricingConfigResponse(global, packageRules));
    }

    @PutMapping("/global-markup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Upsert global percentage markup",
            description = "Sets the platform-wide percentage markup applied to cheapest normalized cost. "
                    + "Previous enabled version is disabled and a new row is inserted (price history). "
                    + "Invalidates catalog caches. Packages without resolvable sell price remain hidden.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Global markup saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<PricingRuleResponse> upsertGlobalMarkup(
            @Valid @RequestBody UpsertGlobalMarkupRequest request) {
        PricingRule rule = upsertGlobalMarkupUseCase.execute(
                new UpsertGlobalMarkupCommand(request.percentage()));
        return ResponseEntity.ok(toResponse(rule));
    }

    @PutMapping("/packages/{packageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Upsert package pricing override",
            description = "PERCENTAGE uses cheapest normalized cost. FIXED sets absolute USD sell price. "
                    + "Previous enabled version is disabled and a new row is inserted (price history). "
                    + "Invalidates catalog caches.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Package pricing saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<PricingRuleResponse> upsertPackagePricing(
            @PathVariable String packageId,
            @Valid @RequestBody UpsertPackagePricingRequest request) {
        PricingRule rule = upsertPackagePricingUseCase.execute(new UpsertPackagePricingCommand(
                packageId, request.type(), request.percentage(), request.fixedPrice()));
        return ResponseEntity.ok(toResponse(rule));
    }

    @DeleteMapping("/packages/{packageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete package pricing override",
            description = "Soft-disables the enabled package override (history retained). "
                    + "Falls back to global markup when present.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Override deleted"),
            @ApiResponse(responseCode = "404", description = "Override not found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<Void> deletePackagePricing(@PathVariable String packageId) {
        boolean deleted = deletePackagePricingUseCase.execute(packageId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private static PricingRuleResponse toResponse(PricingRule rule) {
        return new PricingRuleResponse(
                rule.id(),
                rule.scope(),
                rule.catalogPackageId(),
                rule.type(),
                rule.percentage(),
                rule.fixedPrice(),
                rule.currency(),
                rule.updatedAt());
    }
}
