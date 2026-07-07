package com.peerislands.orders.orders;

import com.peerislands.orders.common.IllegalOrderTransitionException;
import com.peerislands.orders.common.ResourceNotFoundException;
import com.peerislands.orders.inventory.InventoryService;
import com.peerislands.orders.inventory.Product;
import com.peerislands.orders.orders.dto.CreateOrderItemRequest;
import com.peerislands.orders.orders.dto.CreateOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderServiceImpl(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public Order create(CreateOrderRequest request) {
        Instant now = Instant.now();
        Order order = new Order(UUID.randomUUID(), request.getCustomerName(), OrderStatus.PENDING, now, now);
        for (CreateOrderItemRequest line : request.getItems()) {
            Product product = inventoryService.findBySku(line.getProductSku());
            inventoryService.reserve(line.getProductSku(), line.getQuantity());
            OrderItem item = new OrderItem(
                    UUID.randomUUID(),
                    product,
                    product.getName(),
                    product.getUnitPrice(),
                    line.getQuantity());
            order.addItem(item);
        }
        return orderRepository.saveAndFlush(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll(OrderStatus filter) {
        if (filter == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(filter);
    }

    @Override
    @Transactional
    public Order transitionTo(UUID id, OrderStatus target) {
        Order order = get(id);
        order.transitionTo(target);
        if (target == OrderStatus.SHIPPED) {
            for (OrderItem item : order.getItems()) {
                String sku = item.getProduct().getSku();
                inventoryService.finalizeReservation(sku, item.getQuantity());
            }
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancel(UUID id) {
        Order order = get(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalOrderTransitionException(
                    "can only cancel PENDING orders (current status: " + order.getStatus() + ")");
        }
        for (OrderItem item : order.getItems()) {
            String sku = item.getProduct().getSku();
            inventoryService.release(sku, item.getQuantity());
        }
        order.transitionTo(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
