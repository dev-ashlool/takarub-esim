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
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.CartJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.takarub.esim.commerce.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
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
 * Persistence adapter for {@link PaymentAttempt}. May fail to start under H2 if Flyway V13
 * {@code AFTER} syntax is incompatible — that is a pre-existing environment blocker, not a
 * TASK-034C defect. Do not modify V13 to make this boot.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PaymentAttemptRepositoryAdapter.class,
        PaymentAttemptPersistenceMapper.class,
        OrderRepositoryAdapter.class,
        OrderPersistenceMapper.class
})
class PaymentAttemptRepositoryAdapterTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");

    @Autowired
    private PaymentAttemptRepositoryAdapter paymentAdapter;

    @Autowired
    private OrderRepositoryAdapter orderAdapter;

    @Autowired
    private TestEntityManager entityManager;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void savesAndFindsById() {
        Order order = persistOrder();
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());

        paymentAdapter.save(attempt);
        entityManager.flush();
        entityManager.clear();

        Optional<PaymentAttempt> found = paymentAdapter.findById(attempt.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(attempt.id());
        assertThat(found.get().orderId()).isEqualTo(order.id());
        assertThat(found.get().status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(found.get().amount()).isEqualByComparingTo(order.totalAmount());
        assertThat(found.get().currency()).isEqualTo(order.currency());
    }

    @Test
    void findsByOrderIdIncludingMultipleTerminalAttempts() {
        Order order = persistOrder();
        PaymentAttempt failed = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        failed.fail(clock);
        PaymentAttempt confirmed = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        confirmed.assignExternalOrderId(clock, "ext-1");
        confirmed.confirm(clock);

        paymentAdapter.save(failed);
        paymentAdapter.save(confirmed);
        entityManager.flush();
        entityManager.clear();

        List<PaymentAttempt> found = paymentAdapter.findByOrderId(order.id());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(PaymentAttempt::status)
                .containsExactlyInAnyOrder(PaymentAttemptStatus.FAILED, PaymentAttemptStatus.CONFIRMED);
        assertThat(paymentAdapter.findActiveInitiatedByOrderId(order.id())).isEmpty();
    }

    @Test
    void findsActiveInitiatedByOrderId() {
        Order order = persistOrder();
        PaymentAttempt failed = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        failed.fail(clock);
        PaymentAttempt initiated = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());

        paymentAdapter.save(failed);
        paymentAdapter.save(initiated);
        entityManager.flush();
        entityManager.clear();

        Optional<PaymentAttempt> active = paymentAdapter.findActiveInitiatedByOrderId(order.id());

        assertThat(active).isPresent();
        assertThat(active.get().id()).isEqualTo(initiated.id());
        assertThat(active.get().status()).isEqualTo(PaymentAttemptStatus.INITIATED);
    }

    @Test
    void rejectsSecondInitiatedAttemptForSameOrder() {
        Order order = persistOrder();
        PaymentAttempt first = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        paymentAdapter.save(first);
        entityManager.flush();

        PaymentAttempt second = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());

        assertThatThrownBy(() -> {
            paymentAdapter.save(second);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Order persistOrder() {
        UserId userId = persistUser();
        CartId cartId = persistCheckedOutCart(userId);
        Order order = Order.create(
                idGenerator,
                clock,
                cartId,
                userId,
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(snapshot("pkg-pay-1", "USD", "12.50", 2)));
        orderAdapter.save(order);
        entityManager.flush();
        return order;
    }

    private UserId persistUser() {
        String userId = UUID.randomUUID().toString();
        entityManager.persist(new UserJpaEntity(
                userId,
                "pay-" + userId + "@example.com",
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
                quantity,
                "LIKE_CARD",
                "5653",
                new BigDecimal("4.7100"),
                "USD");
    }
}
