package com.takarub.esim.supplier.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Administrative REST endpoints for manual supplier catalog operations during development and testing.
 */
@RestController
@RequestMapping("/api/v1/admin/supplier")
@Tag(name = "Supplier Administration", description = "Manual supplier catalog synchronization for development and testing")
public class AdminSupplierController {

    private final SyncSupplierCatalogUseCase syncSupplierCatalogUseCase;

    public AdminSupplierController(SyncSupplierCatalogUseCase syncSupplierCatalogUseCase) {
        this.syncSupplierCatalogUseCase = syncSupplierCatalogUseCase;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Synchronize supplier catalog",
            description = "Triggers an immediate LikeCard catalog synchronization into the local catalog store. "
                    + "Intended for development and testing; the background scheduler remains the production path.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Synchronization completed",
                    content = @Content(schema = @Schema(implementation = SyncSupplierCatalogResult.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role"),
            @ApiResponse(responseCode = "400", description = "Invalid synchronization request")
    })
    public ResponseEntity<SyncSupplierCatalogResult> syncCatalog() {
        SyncSupplierCatalogResult result =
                syncSupplierCatalogUseCase.execute(SyncSupplierCatalogCommand.forLikeCard());
        return ResponseEntity.ok(result);
    }
}
