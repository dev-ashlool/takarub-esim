package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.CartJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Integration test ({@code @DataJpaTest}, H2 in MySQL mode, Flyway-built schema) for the Order
 * repository adapter. Named {@code *Test} so it runs in the Surefire test phase.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrderRepositoryAdapter.class, OrderPersistenceMapper.class})
class OrderRepositoryAdapterTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");

    @Autowired
    private OrderRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void savesAndReloadsByIdWithHeaderAndItems() {
        UserId userId = persistUser();
        CartId cartId = persistCheckedOutCart(userId);
        OrderItemSnapshot line = snapshot("pkg-save-1", "USD", "12.50", 2);
        Order order = Order.create(idGenerator, clock, cartId, userId, List.of(line));

        adapter.save(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = adapter.findById(order.id());

        assertThat(found).isPresent();
        Order reloaded = found.get();
        assertThat(reloaded.id()).isEqualTo(order.id());
        assertThat(reloaded.cartId()).isEqualTo(cartId);
        assertThat(reloaded.userId()).isEqualTo(userId);
        assertThat(reloaded.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(reloaded.totalAmount()).isEqualByComparingTo("25.00");
        assertThat(reloaded.currency()).isEqualTo("USD");
        assertThat(reloaded.createdAt()).isEqualTo(FIXED);
        assertThat(reloaded.updatedAt()).isEqualTo(FIXED);
        assertThat(reloaded.itemsView()).hasSize(1);
        assertThat(reloaded.itemsView().get(0).packageId()).isEqualTo("pkg-save-1");
        assertThat(reloaded.itemsView().get(0).countryIso()).isEqualTo("TR");
        assertThat(reloaded.itemsView().get(0).countryNameArabic()).isEqualTo("تركيا");
        assertThat(reloaded.itemsView().get(0).countryNameEnglish()).isEqualTo("Turkey");
        assertThat(reloaded.itemsView().get(0).locationType()).isEqualTo(LocationType.COUNTRY);
        assertThat(reloaded.itemsView().get(0).dataAmount()).isEqualTo(5);
        assertThat(reloaded.itemsView().get(0).dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(reloaded.itemsView().get(0).durationDays()).isEqualTo(7);
        assertThat(reloaded.itemsView().get(0).unitPrice()).isEqualByComparingTo("12.50");
        assertThat(reloaded.itemsView().get(0).currency()).isEqualTo("USD");
        assertThat(reloaded.itemsView().get(0).quantity()).isEqualTo(2);
        assertThat(reloaded.itemsView().get(0).lineTotal()).isEqualByComparingTo("25.00");
    }

    @Test
    void findsByCartIdWithItemsLoaded() {
        UserId userId = persistUser();
        CartId cartId = persistCheckedOutCart(userId);
        Order order = Order.create(
                idGenerator,
                clock,
                cartId,
                userId,
                List.of(
                        snapshot("pkg-a", "USD", "10.00", 1),
                        snapshot("pkg-b", "USD", "3.25", 4)));

        adapter.save(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = adapter.findByCartId(cartId);

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(order.id());
        assertThat(found.get().itemsView()).hasSize(2);
        assertThat(found.get().totalAmount()).isEqualByComparingTo("23.00");
    }

    @Test
    void rejectsSecondOrderForSameCartId() {
        UserId userId = persistUser();
        CartId cartId = persistCheckedOutCart(userId);

        adapter.save(Order.create(idGenerator, clock, cartId, userId, List.of(snapshot("pkg-1", "USD", "9.99", 1))));
        entityManager.flush();

        Order second = Order.create(idGenerator, clock, cartId, userId, List.of(snapshot("pkg-2", "USD", "5.00", 1)));

        assertThatThrownBy(() -> {
            adapter.save(second);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UserId persistUser() {
        String userId = UUID.randomUUID().toString();
        entityManager.persist(new UserJpaEntity(
                userId,
                "order-" + userId + "@example.com",
                "$2a$10$hash",
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER),
                FIXED,
                FIXED));
        entityManager.flush();
        return UserId.of(userId);
    }

    private CartId persistCheckedOutCart(UserId userId) {
        String cartId = UUID.randomUUID().toString();
        entityManager.persist(new CartJpaEntity(
                cartId,
                userId.value().toString(),
                CartStatus.CHECKED_OUT,
                FIXED,
                FIXED));
        entityManager.flush();
        return CartId.of(cartId);
    }

    private static OrderItemSnapshot snapshot(String packageId, String currency, String unitPrice, int quantity) {
        return new OrderItemSnapshot(
                packageId,
                "TR",
                "تركيا",
                "Turkey",
                LocationType.COUNTRY,
                5,
                DataUnit.GB,
                7,
                new BigDecimal(unitPrice),
                currency,
                quantity);
    }
}
