package com.peerislands.orders.orders;

import com.peerislands.orders.common.InsufficientStockException;
import com.peerislands.orders.common.ResourceNotFoundException;
import com.peerislands.orders.inventory.InventoryService;
import com.peerislands.orders.inventory.Product;
import com.peerislands.orders.inventory.dto.UpdateProductRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceImplTest {
    @Autowired OrderService orderService;
    @Autowired InventoryService inventoryService;
    @Autowired OrderRepository orderRepository;

    private String productSku;

    @BeforeEach
    void seedProduct() {
        com.peerislands.orders.inventory.dto.CreateProductRequest r = new com.peerislands.orders.inventory.dto.CreateProductRequest();
        r.setName("Widget");
        r.setSku("W-1");
        r.setAvailableStock(10);
        r.setUnitPrice(new BigDecimal("5.00"));
        inventoryService.create(r);
        productSku = "W-1";
    }

    private CreateOrderRequest orderRequest(int qty) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Alice");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku(productSku);
        item.setQuantity(qty);
        req.setItems(List.of(item));
        return req;
    }

    @Test
    void createHappyPathReservesStockAndPersists() {
        Order o = orderService.create(orderRequest(2));
        assertThat(o.getId()).isNotNull();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(o.getItems()).hasSize(1);

        Product refreshed = inventoryService.findBySku(productSku);
        assertThat(refreshed.getAvailableStock()).isEqualTo(8);
        assertThat(refreshed.getReservedStock()).isEqualTo(2);
    }

    @Test
    void createWithUnknownSkuThrows404() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName("Bob");
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSku("NOPE");
        item.setQuantity(1);
        req.setItems(List.of(item));
        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createWithInsufficientStockThrows409AndDoesNotMutate() {
        assertThatThrownBy(() -> orderService.create(orderRequest(999)))
                .isInstanceOf(InsufficientStockException.class);
        Product refreshed = inventoryService.findBySku(productSku);
        assertThat(refreshed.getAvailableStock()).isEqualTo(10);
        assertThat(refreshed.getReservedStock()).isEqualTo(0);
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void createSnapshotsProductNameAndPrice() {
        Order o = orderService.create(orderRequest(1));
        OrderItem snapshot = o.getItems().get(0);
        assertThat(snapshot.getProductName()).isEqualTo("Widget");
        assertThat(snapshot.getUnitPrice()).isEqualByComparingTo("5.00");

        Product p = inventoryService.findBySku(productSku);
        UpdateProductRequest u = new UpdateProductRequest();
        u.setName("Widget Pro");
        u.setAvailableStock(50);
        u.setUnitPrice(new BigDecimal("12.50"));
        inventoryService.update(p.getId(), u);
        Order reloaded = orderService.get(o.getId());
        assertThat(reloaded.getItems().get(0).getProductName()).isEqualTo("Widget");
        assertThat(reloaded.getItems().get(0).getUnitPrice()).isEqualByComparingTo("5.00");
    }
}
