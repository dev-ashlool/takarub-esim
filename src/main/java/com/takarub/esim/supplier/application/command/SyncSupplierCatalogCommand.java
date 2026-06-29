package com.takarub.esim.supplier.application.command;

/**
 * Command to synchronize a supplier remote catalog into the local catalog store.
 */
public record SyncSupplierCatalogCommand(String supplierKey) {

    public static SyncSupplierCatalogCommand forLikeCard() {
        return new SyncSupplierCatalogCommand("LIKE_CARD");
    }
}
