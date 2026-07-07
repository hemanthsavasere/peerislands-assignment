package com.peerislands.orders.orders;

import com.peerislands.orders.common.IllegalOrderTransitionException;
import com.peerislands.orders.inventory.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    @Test
    void transitionToIllegalThrows() {
        Order o = new Order(UUID.randomUUID(), "Alice", OrderStatus.PENDING, Instant.now(), Instant.now());
        assertThatThrownBy(() -> o.transitionTo(OrderStatus.DELIVERED))
                .isInstanceOf(IllegalOrderTransitionException.class);
    }

    @Test
    void transitionToLegalMutatesStatus() {
        Order o = new Order(UUID.randomUUID(), "Alice", OrderStatus.PENDING, Instant.now(), Instant.now());
        o.transitionTo(OrderStatus.PROCESSING);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void addItemBindsBothSides() {
        Order o = new Order(UUID.randomUUID(), "Alice", OrderStatus.PENDING, Instant.now(), Instant.now());
        OrderItem item = new OrderItem(UUID.randomUUID(), null, "Widget", new BigDecimal("9.99"), 1);
        o.addItem(item);
        assertThat(o.getItems()).contains(item);
        assertThat(item.getOrder()).isSameAs(o);
    }

    @Test
    void snapshotsDoNotChangeWhenProductEdits() {
        Product p = new Product(UUID.randomUUID(), "Widget", "W-1", new BigDecimal("9.99"), 10, 0);
        OrderItem item = new OrderItem(UUID.randomUUID(), p, p.getName(), p.getUnitPrice(), 2);
        Order o = new Order(UUID.randomUUID(), "Alice", OrderStatus.PENDING, Instant.now(), Instant.now());
        o.addItem(item);

        p.setName("Widget Pro");
        p.setUnitPrice(new BigDecimal("15.00"));

        assertThat(item.getProductName()).isEqualTo("Widget");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("9.99");
    }
}
