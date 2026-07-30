package com.takarub.esim.commerce.domain.cart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Internal cart line: commercial offer snapshot plus quantity. Created and mutated only by
 * {@link Cart}. {@code lineTotal} is derived and never stored as state.
 */
public class CartItem {

    private static final int MONEY_SCALE = 2;

    private final String packageId;
    private String countryIso;
    private String countryNameArabic;
    private String countryNameEnglish;
    private LocationType locationType;
    private int dataAmount;
    private DataUnit dataUnit;
    private int durationDays;
    private BigDecimal unitPrice;
    private String currency;
    private int quantity;

    CartItem(CartItemOffer offer, int quantity) {
        validateOffer(offer);
        validateQuantity(quantity);
        this.packageId = offer.packageId().trim();
        applyOffer(offer);
        this.quantity = quantity;
    }

    /**
     * Rebuilds a line from persisted state without re-running merge rules.
     */
    static CartItem reconstitute(
            String packageId,
            String countryIso,
            String countryNameArabic,
            String countryNameEnglish,
            LocationType locationType,
            int dataAmount,
            DataUnit dataUnit,
            int durationDays,
            BigDecimal unitPrice,
            String currency,
            int quantity) {
        CartItemOffer offer = new CartItemOffer(
                packageId,
                countryIso,
                countryNameArabic,
                countryNameEnglish,
                locationType,
                dataAmount,
                dataUnit,
                durationDays,
                unitPrice,
                currency);
        return new CartItem(offer, quantity);
    }

    void mergeAdditional(CartItemOffer offer, int additionalQuantity) {
        validateOffer(offer);
        validateQuantity(additionalQuantity);
        if (!this.packageId.equals(offer.packageId().trim())) {
            throw new ValidationException("Cannot merge cart lines for different packages");
        }
        applyOffer(offer);
        this.quantity = this.quantity + additionalQuantity;
    }

    void replaceQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    private void applyOffer(CartItemOffer offer) {
        this.countryIso = offer.countryIso().trim();
        this.countryNameArabic = offer.countryNameArabic().trim();
        this.countryNameEnglish = offer.countryNameEnglish().trim();
        this.locationType = offer.locationType();
        this.dataAmount = offer.dataAmount();
        this.dataUnit = offer.dataUnit();
        this.durationDays = offer.durationDays();
        this.unitPrice = scaleMoney(offer.unitPrice());
        this.currency = offer.currency().trim();
    }

    static void validateOffer(CartItemOffer offer) {
        if (offer == null) {
            throw new ValidationException("Cart item offer is required");
        }
        requireText(offer.packageId(), "packageId");
        requireText(offer.countryIso(), "countryIso");
        requireText(offer.countryNameArabic(), "countryNameArabic");
        requireText(offer.countryNameEnglish(), "countryNameEnglish");
        if (offer.locationType() == null) {
            throw new ValidationException("locationType is required");
        }
        if (offer.dataAmount() < 1) {
            throw new ValidationException("dataAmount must be at least 1");
        }
        if (offer.dataUnit() == null) {
            throw new ValidationException("dataUnit is required");
        }
        if (offer.durationDays() < 1) {
            throw new ValidationException("durationDays must be at least 1");
        }
        if (offer.unitPrice() == null || offer.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("unitPrice must be positive");
        }
        requireText(offer.currency(), "currency");
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("quantity must be positive");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    private static BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public String packageId() {
        return packageId;
    }

    public String countryIso() {
        return countryIso;
    }

    public String countryNameArabic() {
        return countryNameArabic;
    }

    public String countryNameEnglish() {
        return countryNameEnglish;
    }

    public LocationType locationType() {
        return locationType;
    }

    public int dataAmount() {
        return dataAmount;
    }

    public DataUnit dataUnit() {
        return dataUnit;
    }

    public int durationDays() {
        return durationDays;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public String currency() {
        return currency;
    }

    public int quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CartItem cartItem)) {
            return false;
        }
        return Objects.equals(packageId, cartItem.packageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageId);
    }
}
