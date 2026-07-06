package com.peerislands.orders.inventory;

import com.peerislands.orders.common.DuplicateSkuException;
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
}
