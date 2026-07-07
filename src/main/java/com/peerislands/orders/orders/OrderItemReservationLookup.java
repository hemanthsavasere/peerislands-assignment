package com.peerislands.orders.orders;

import com.peerislands.orders.inventory.ReservationLookup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderItemReservationLookup implements ReservationLookup {
    private final OrderItemRepository orderItemRepository;

    public OrderItemReservationLookup(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasActiveReservation(UUID productId) {
        return orderItemRepository.existsByProductIdAndOrderStatusIn(
                productId, List.of(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }
}
