package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a catalog package is commercially sellable but no in-stock supplier mapping can be
 * selected for checkout freeze.
 */
public class NoSupplierProductAvailableApplicationException extends BusinessException {

    public NoSupplierProductAvailableApplicationException(String packageId) {
        super(CommerceApplicationErrorCode.NO_SUPPLIER_PRODUCT_AVAILABLE,
                "No in-stock supplier product is available for package: " + packageId);
    }
}
