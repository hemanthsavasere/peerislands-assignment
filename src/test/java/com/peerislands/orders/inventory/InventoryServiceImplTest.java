package com.peerislands.orders.inventory;

import com.peerislands.orders.common.DuplicateSkuException;
import com.peerislands.orders.common.InsufficientStockException;
import com.peerislands.orders.common.ResourceNotFoundException;
import com.peerislands.orders.inventory.dto.CreateProductRequest;
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
class InventoryServiceImplTest {
    @Autowired InventoryService inventoryService;
    @Autowired ProductRepository productRepository;

    private CreateProductRequest createRequest(String sku) {
        CreateProductRequest r = new CreateProductRequest();
        r.setName("Widget");
        r.setSku(sku);
        r.setAvailableStock(100);
        r.setUnitPrice(new BigDecimal("9.99"));
        return r;
    }

    @Test
    void createPersistsAndReturnsProduct() {
        Product p = inventoryService.create(createRequest("W-1"));
        assertThat(p.getId()).isNotNull();
        assertThat(productRepository.findBySku("W-1")).isPresent();
    }

    @Test
    void createThrowsDuplicateSku() {
        inventoryService.create(createRequest("W-DUP"));
        assertThatThrownBy(() -> inventoryService.create(createRequest("W-DUP")))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void findBySkuThrowsWhenMissing() {
        assertThatThrownBy(() -> inventoryService.findBySku("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllReturnsAll() {
        inventoryService.create(createRequest("W-A"));
        inventoryService.create(createRequest("W-B"));
        List<Product> all = inventoryService.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void reserveMovesStock() {
        inventoryService.create(createRequest("W-R"));
        inventoryService.reserve("W-R", 10);
        Product p = inventoryService.findBySku("W-R");
        assertThat(p.getAvailableStock()).isEqualTo(90);
        assertThat(p.getReservedStock()).isEqualTo(10);
    }

    @Test
    void reserveThrowsInsufficientStockAndDoesNotMutate() {
        inventoryService.create(createRequest("W-S"));
        assertThatThrownBy(() -> inventoryService.reserve("W-S", 999))
                .isInstanceOf(InsufficientStockException.class);
        Product p = inventoryService.findBySku("W-S");
        assertThat(p.getAvailableStock()).isEqualTo(100);
        assertThat(p.getReservedStock()).isEqualTo(0);
    }

    @Test
    void releaseMovesStockBack() {
        inventoryService.create(createRequest("W-L"));
        inventoryService.reserve("W-L", 10);
        inventoryService.release("W-L", 4);
        Product p = inventoryService.findBySku("W-L");
        assertThat(p.getAvailableStock()).isEqualTo(94);
        assertThat(p.getReservedStock()).isEqualTo(6);
    }

    @Test
    void finalizeReservationDecrementsReservedOnly() {
        inventoryService.create(createRequest("W-F"));
        inventoryService.reserve("W-F", 10);
        inventoryService.finalizeReservation("W-F", 10);
        Product p = inventoryService.findBySku("W-F");
        assertThat(p.getAvailableStock()).isEqualTo(90);
        assertThat(p.getReservedStock()).isEqualTo(0);
    }
}
