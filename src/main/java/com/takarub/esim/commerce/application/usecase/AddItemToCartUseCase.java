package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.commerce.application.command.AddItemToCartCommand;
import com.takarub.esim.commerce.application.exception.PackageNotSellableApplicationException;
import com.takarub.esim.commerce.application.result.CartView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartItemOffer;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Adds a catalog package to the caller's open cart.
 *
 * <p>Orchestration: load package (Catalog, sell price already resolved via Pricing), assemble
 * {@link CartItemOffer}, find or create the open cart, invoke the aggregate, persist.
 */
public class AddItemToCartUseCase {

    private final TransactionRunner transactionRunner;
    private final CartRepository cartRepository;
    private final CatalogBrowsePort catalogBrowsePort;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public AddItemToCartUseCase(TransactionRunner transactionRunner,
                                CartRepository cartRepository,
                                CatalogBrowsePort catalogBrowsePort,
                                IdGenerator idGenerator,
                                ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.cartRepository = cartRepository;
        this.catalogBrowsePort = catalogBrowsePort;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public CartView execute(AddItemToCartCommand command) {
        return transactionRunner.execute(() -> {
            PackageDetailsView packageDetails = loadSellablePackage(command.packageId());
            CartItemOffer offer = toOffer(packageDetails);

            UserId userId = UserId.of(command.userId());
            Cart cart = OpenCartLoader.findOrCreate(cartRepository, idGenerator, clock, userId);
            cart.addItem(offer, command.quantity(), clock);
            Cart saved = cartRepository.save(cart);
            return CartView.from(saved);
        });
    }

    private PackageDetailsView loadSellablePackage(String packageId) {
        PackageDetailsView details = catalogBrowsePort.findPackageById(packageId)
                .orElseThrow(() -> new PackageNotSellableApplicationException(packageId));
        if (!details.available()) {
            throw new PackageNotSellableApplicationException(packageId);
        }
        return details;
    }

    private static CartItemOffer toOffer(PackageDetailsView pkg) {
        return new CartItemOffer(
                pkg.id(),
                pkg.countryIso(),
                pkg.countryArabicName(),
                pkg.countryEnglishName(),
                pkg.locationType(),
                pkg.dataAmount(),
                pkg.dataUnit(),
                pkg.durationDays(),
                pkg.price(),
                pkg.priceCurrency());
    }
}
