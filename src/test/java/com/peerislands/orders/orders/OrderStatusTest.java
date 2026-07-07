package com.peerislands.orders.orders;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {
    @Test
    void pendingToProcessingIsLegal() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
    }

    @Test
    void processingToShippedIsLegal() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
    }

    @Test
    void shippedToDeliveredIsLegal() {
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void pendingToCancelledIsLegal() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void shippedToPendingIsIllegal() {
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    void deliveredToCancelledIsIllegal() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void processingToPendingIsIllegal() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    void cancelledToAnythingIsIllegal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).as("CANCELLED -> %s", target).isFalse();
        }
    }

    @Test
    void processingToDeliveredIsIllegal() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void pendingToShippedIsIllegal() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
    }
}
