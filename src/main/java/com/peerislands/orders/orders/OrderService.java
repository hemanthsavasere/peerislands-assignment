package com.peerislands.orders.orders;

import com.peerislands.orders.orders.dto.CreateOrderRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    Order create(CreateOrderRequest request);
    Order get(UUID id);
    List<Order> findAll(OrderStatus filter);
    Order transitionTo(UUID id, OrderStatus target);
    Order cancel(UUID id);
}
