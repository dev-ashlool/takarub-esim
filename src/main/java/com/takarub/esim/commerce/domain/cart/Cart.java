package com.takarub.esim.commerce.domain.cart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Cart aggregate root. Owns shopping intent for one {@link UserId}: open/checked-out lifecycle and
 * commercial line snapshots. Does not call Catalog or Pricing; the application supplies validated
 * {@link CartItemOffer} data.
 */
public class Cart {

    private final CartId id;
    private final UserId userId;
    private CartStatus status;
    private final List<CartItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    private Cart(CartId id, Instant createdAt, UserId userId, CartStatus status, List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.items = new ArrayList<>(items);
    }

    private Cart(CartId id, Instant createdAt, Instant updatedAt, UserId userId, CartStatus status,
                 List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.items = new ArrayList<>(items);
    }

    /**
     * Creates an empty {@link CartStatus#OPEN} cart for the given user.
     */
    public static Cart create(IdGenerator idGenerator, ClockProvider clock, UserId userId) {
        if (userId == null) {
            throw new ValidationException("User id is required to create a cart");
        }
        Instant now = clock.now();
        return new Cart(CartId.generate(idGenerator), now, userId, CartStatus.OPEN, List.of());
    }

    /**
     * Rebuilds a cart from persisted state without running create/add rules. For exclusive use by
     * the infrastructure persistence mapper.
     */
    public static Cart reconstitute(CartId id, Instant createdAt, Instant updatedAt, UserId userId,
                                    CartStatus status, List<CartItem> items) {
        return new Cart(id, createdAt, updatedAt, userId, status,
                items == null ? List.of() : items);
    }

    public void addItem(CartItemOffer offer, int quantity, ClockProvider clock) {
        ensureOpen();
        CartItem.validateOffer(offer);
        if (quantity <= 0) {
            throw new ValidationException("quantity must be positive");
        }

        Optional<CartItem> existing = findItem(offer.packageId().trim());
        if (existing.isPresent()) {
            existing.get().mergeAdditional(offer, quantity);
        } else {
            items.add(new CartItem(offer, quantity));
        }
        touch(clock);
    }

    public void updateQuantity(String packageId, int quantity, ClockProvider clock) {
        ensureOpen();
        requirePackageId(packageId);
        CartItem item = requireItem(packageId.trim());
        item.replaceQuantity(quantity);
        touch(clock);
    }

    public void removeItem(String packageId, ClockProvider clock) {
        ensureOpen();
        requirePackageId(packageId);
        boolean removed = items.removeIf(item -> item.packageId().equals(packageId.trim()));
        if (!removed) {
            throw new ValidationException("Cart item not found for packageId: " + packageId.trim());
        }
        touch(clock);
    }

    public void validateNotEmpty() {
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
    }

    /**
     * Transitions an open, non-empty cart to {@link CartStatus#CHECKED_OUT}. Intended for use by a
     * future Checkout use case; no Order or Payment behavior lives here.
     */
    public void checkout(ClockProvider clock) {
        if (status == CartStatus.CHECKED_OUT) {
            throw new ConflictException("Cart is already checked out");
        }
        ensureOpen();
        validateNotEmpty();
        this.status = CartStatus.CHECKED_OUT;
        touch(clock);
    }

    public void ensureOpen() {
        if (status != CartStatus.OPEN) {
            throw new ConflictException("Cart cannot be modified because it is not open");
        }
    }

    public List<CartItem> itemsView() {
        return Collections.unmodifiableList(items);
    }

    public CartId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public CartStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private Optional<CartItem> findItem(String packageId) {
        return items.stream().filter(item -> item.packageId().equals(packageId)).findFirst();
    }

    private CartItem requireItem(String packageId) {
        return findItem(packageId).orElseThrow(
                () -> new ValidationException("Cart item not found for packageId: " + packageId));
    }

    private static void requirePackageId(String packageId) {
        if (packageId == null || packageId.isBlank()) {
            throw new ValidationException("packageId is required");
        }
    }

    private void touch(ClockProvider clock) {
        this.updatedAt = clock.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cart cart)) {
            return false;
        }
        return Objects.equals(id, cart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
