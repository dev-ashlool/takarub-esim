package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a catalog package cannot be sold into a cart (missing, no sell price, or unavailable).
 */
public class PackageNotSellableApplicationException extends BusinessException {

    public PackageNotSellableApplicationException(String packageId) {
        super(CommerceApplicationErrorCode.PACKAGE_NOT_SELLABLE,
                "Catalog package is not sellable: " + packageId);
    }
}
