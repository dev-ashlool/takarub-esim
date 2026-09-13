package com.takarub.esim.commerce.presentation.admin.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.usecase.SearchAdminPurchasesUseCase;
import com.takarub.esim.commerce.presentation.admin.response.AdminPurchaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/purchases")
@Tag(name = "Purchase Administration", description = "Admin purchase search by supplier and customer filters")
public class AdminPurchaseController {

    private final SearchAdminPurchasesUseCase searchAdminPurchasesUseCase;

    public AdminPurchaseController(SearchAdminPurchasesUseCase searchAdminPurchasesUseCase) {
        this.searchAdminPurchasesUseCase = searchAdminPurchasesUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search purchases for admin")
    public ResponseEntity<AdminPurchaseResponse> search(
            @Parameter(description = "Supplier key, e.g. LIKE_CARD")
            @RequestParam(name = "supplierId", required = false) String supplierId,
            @RequestParam(name = "supplierKey", required = false) String supplierKey,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "packageName", required = false) String packageName,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        String resolvedSupplier = supplierKey != null && !supplierKey.isBlank() ? supplierKey : supplierId;
        return ResponseEntity.ok(AdminPurchaseResponse.from(searchAdminPurchasesUseCase.execute(
                new SearchAdminPurchasesQuery(
                        resolvedSupplier, from, to, packageName, phone, email, name, page, size))));
    }
}
