package com.takarub.esim.commerce.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Immutable commercial order line. Created only by {@link Order}; {@code lineTotal} is derived and
 * never stored as independent mutable state.
 */
public class OrderItem {

    private static final int MONEY_SCALE = 2;

    private final String packageId;
    private final String countryIso;
    private final String countryNameArabic;
    private final String countryNameEnglish;
    private final LocationType locationType;
    private final int dataAmount;
    private final DataUnit dataUnit;
    private final int durationDays;
    private final BigDecimal unitPrice;
    private final String currency;
    private final int quantity;

    OrderItem(OrderItemSnapshot snapshot) {
        validateSnapshot(snapshot);
        this.packageId = snapshot.packageId().trim();
        this.countryIso = snapshot.countryIso().trim();
        this.countryNameArabic = snapshot.countryNameArabic().trim();
        this.countryNameEnglish = snapshot.countryNameEnglish().trim();
        this.locationType = snapshot.locationType();
        this.dataAmount = snapshot.dataAmount();
        this.dataUnit = snapshot.dataUnit();
        this.durationDays = snapshot.durationDays();
        this.unitPrice = scaleMoney(snapshot.unitPrice());
        this.currency = snapshot.currency().trim();
        this.quantity = snapshot.quantity();
    }

    /**
     * Rebuilds a line from persisted state without re-running create orchestration.
     * Public so the infrastructure persistence mapper can reconstitute order lines.
     */
    public static OrderItem reconstitute(
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
        OrderItemSnapshot snapshot = new OrderItemSnapshot(
                packageId,
                countryIso,
                countryNameArabic,
                countryNameEnglish,
                locationType,
                dataAmount,
                dataUnit,
                durationDays,
                unitPrice,
                currency,
                quantity);
        return new OrderItem(snapshot);
    }

    static void validateSnapshot(OrderItemSnapshot snapshot) {
        if (snapshot == null) {
            throw new ValidationException("Order item snapshot is required");
        }
        requireText(snapshot.packageId(), "packageId");
        requireText(snapshot.countryIso(), "countryIso");
        requireText(snapshot.countryNameArabic(), "countryNameArabic");
        requireText(snapshot.countryNameEnglish(), "countryNameEnglish");
        if (snapshot.locationType() == null) {
            throw new ValidationException("locationType is required");
        }
        if (snapshot.dataAmount() < 1) {
            throw new ValidationException("dataAmount must be at least 1");
        }
        if (snapshot.dataUnit() == null) {
            throw new ValidationException("dataUnit is required");
        }
        if (snapshot.durationDays() < 1) {
            throw new ValidationException("durationDays must be at least 1");
        }
        if (snapshot.unitPrice() == null || snapshot.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("unitPrice must be positive");
        }
        requireText(snapshot.currency(), "currency");
        if (snapshot.quantity() <= 0) {
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
        if (!(o instanceof OrderItem orderItem)) {
            return false;
        }
        return Objects.equals(packageId, orderItem.packageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageId);
    }
}
