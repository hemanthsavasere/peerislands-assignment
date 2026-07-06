package com.peerislands.orders.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {
    @Autowired
    ProductRepository repository;

    @Test
    void savesAndFindBySku() {
        UUID id = UUID.randomUUID();
        Product p = new Product(id, "Widget", "W-1", new BigDecimal("9.99"), 100, 0);
        repository.save(p);

        Optional<Product> found = repository.findBySku("W-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Widget");
    }

    @Test
    void existsBySkuReturnsTrue() {
        repository.save(new Product(UUID.randomUUID(), "Widget", "W-1", new BigDecimal("9.99"), 100, 0));
        assertThat(repository.existsBySku("W-1")).isTrue();
        assertThat(repository.existsBySku("NOPE")).isFalse();
    }

    @Test
    void concurrentUpdateWithStaleVersionThrowsOptimisticException() {
        UUID id = UUID.randomUUID();
        repository.save(new Product(id, "Widget", "W-1", new BigDecimal("9.99"), 100, 0));

        Product loaded = repository.findById(id).orElseThrow();
        loaded.setAvailableStock(50);
        repository.saveAndFlush(loaded);

        Product sameLoaded = repository.findById(id).orElseThrow();
        sameLoaded.setAvailableStock(40);
        repository.saveAndFlush(sameLoaded);

        Product stale = new Product(id, "Stale", "W-1", new BigDecimal("9.99"), 99, 0);
        org.springframework.test.util.ReflectionTestUtils.setField(stale, "version", 0L);
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }
}
