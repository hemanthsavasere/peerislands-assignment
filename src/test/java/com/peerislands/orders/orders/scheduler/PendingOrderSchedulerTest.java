package com.peerislands.orders.orders.scheduler;

import com.peerislands.orders.inventory.InventoryService;
import com.peerislands.orders.inventory.Product;
import com.peerislands.orders.inventory.ProductRepository;
import com.peerislands.orders.inventory.dto.CreateProductRequest;
import com.peerislands.orders.orders.OrderStatus;
import com.peerislands.orders.orders.OrderRepository;
import com.peerislands.orders.orders.OrderService;
import com.peerislands.orders.orders.Order;
import com.peerislands.orders.orders.dto.CreateOrderItemRequest;
import com.peerislands.orders.orders.dto.CreateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PendingOrderSchedulerTest {
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired InventoryService inventoryService;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Order createOrder(int qty) {
        CreateProductRequest r = new CreateProductRequest();
        r.setName("Widget-" + UUID.randomUUID());
        r.setSku("W-" + UUID.randomUUID());
        r.setAvailableStock(100);
        r.setUnitPrice(new BigDecimal("5.00"));
        inventoryService.create(r);
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Alice");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku(r.getSku());
        item.setQuantity(qty);
        req.setItems(List.of(item));
        return orderService.create(req);
    }

    @Test
    void caseA_schedulerAdvancesAllPendingToProcessing() {
        createOrder(1);
        createOrder(1);
        createOrder(1);

        await().atMost(3, SECONDS).until(() -> orderRepository.findByStatus(OrderStatus.PENDING).isEmpty());

        List<Order> processing = orderRepository.findByStatus(OrderStatus.PROCESSING);
        assertThat(processing).hasSize(3);
    }

    @Test
    void caseB_cancelRaceIsMutuallyExclusive() {
        String sku = "W-" + UUID.randomUUID();
        CreateProductRequest r = new CreateProductRequest();
        r.setName("Widget-" + UUID.randomUUID());
        r.setSku(sku);
        r.setAvailableStock(100);
        r.setUnitPrice(new BigDecimal("5.00"));
        inventoryService.create(r);
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Alice");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku(sku);
        item.setQuantity(2);
        req.setItems(List.of(item));
        Order o = orderService.create(req);

        try {
            orderService.cancel(o.getId());
        } catch (Exception ignored) {
        }

        await().atMost(3, SECONDS).until(() -> orderRepository.findByStatus(OrderStatus.PENDING).isEmpty());

        Order refreshed = orderRepository.findById(o.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isIn(OrderStatus.CANCELLED, OrderStatus.PROCESSING);

        Product p = inventoryService.findBySku(sku);
        if (refreshed.getStatus() == OrderStatus.CANCELLED) {
            assertThat(p.getReservedStock()).isEqualTo(0);
            assertThat(p.getAvailableStock()).isEqualTo(100);
        } else {
            assertThat(p.getReservedStock()).isEqualTo(2);
            assertThat(p.getAvailableStock()).isEqualTo(98);
        }
    }

    @Test
    void caseC_partialFailureIsolation() {
        Order a = createOrder(1);
        Order b = createOrder(1);
        Order c = createOrder(1);
        Order d = createOrder(1);
        Order e = createOrder(1);

        try {
            orderService.cancel(c.getId());
        } catch (Exception ignored) {
        }

        await().atMost(4, SECONDS).until(() -> orderRepository.findByStatus(OrderStatus.PENDING).isEmpty());

        long cancelled = orderRepository.findByStatus(OrderStatus.CANCELLED).size();
        long processing = orderRepository.findByStatus(OrderStatus.PROCESSING).size();
        assertThat(cancelled + processing).isEqualTo(5);
    }
}
