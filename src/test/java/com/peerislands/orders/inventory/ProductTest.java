package com.peerislands.orders.inventory;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {
    @Test
    void constructorSetsAllFieldsWithVersionZero() {
        UUID id = UUID.randomUUID();
        Product p = new Product(id, "Widget", "W-1", new BigDecimal("9.99"), 100, 0);
        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getName()).isEqualTo("Widget");
        assertThat(p.getSku()).isEqualTo("W-1");
        assertThat(p.getUnitPrice()).isEqualByComparingTo("9.99");
        assertThat(p.getAvailableStock()).isEqualTo(100);
        assertThat(p.getReservedStock()).isEqualTo(0);
        assertThat(p.getVersion()).isEqualTo(0L);
    }

    @Test
    void setAvailableStockRejectsNegative() {
        Product p = new Product(UUID.randomUUID(), "x", "s", new BigDecimal("1.00"), 10, 0);
        assertThatThrownBy(() -> p.setAvailableStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setReservedStockRejectsNegative() {
        Product p = new Product(UUID.randomUUID(), "x", "s", new BigDecimal("1.00"), 10, 0);
        assertThatThrownBy(() -> p.setReservedStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reserveMovesStockFromAvailableToReserved() {
        Product p = new Product(UUID.randomUUID(), "x", "s", new BigDecimal("1.00"), 10, 0);
        p.reserve(3);
        assertThat(p.getAvailableStock()).isEqualTo(7);
        assertThat(p.getReservedStock()).isEqualTo(3);
    }

    @Test
    void releaseMovesStockFromReservedToAvailable() {
        Product p = new Product(UUID.randomUUID(), "x", "s", new BigDecimal("1.00"), 7, 3);
        p.release(2);
        assertThat(p.getAvailableStock()).isEqualTo(9);
        assertThat(p.getReservedStock()).isEqualTo(1);
    }

    @Test
    void finalizeReservationDecrementsReservedOnly() {
        Product p = new Product(UUID.randomUUID(), "x", "s", new BigDecimal("1.00"), 7, 3);
        p.finalizeReservation(3);
        assertThat(p.getAvailableStock()).isEqualTo(7);
        assertThat(p.getReservedStock()).isEqualTo(0);
    }
}
