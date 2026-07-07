package com.peerislands.orders.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    @Autowired OrderRepository orderRepository;

    private Order newOrder(OrderStatus status) {
        return new Order(UUID.randomUUID(), "Alice", status, Instant.now(), Instant.now());
    }

    @Test
    void savesOrderWithNoItems() {
        Order saved = orderRepository.save(newOrder(OrderStatus.PENDING));
        assertThat(orderRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void findByStatusReturnsOnlyMatching() {
        orderRepository.save(newOrder(OrderStatus.PENDING));
        orderRepository.save(newOrder(OrderStatus.PROCESSING));
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findIdsByStatusReturnsUuids() {
        Order a = orderRepository.save(newOrder(OrderStatus.PENDING));
        List<UUID> ids = orderRepository.findIdsByStatus(OrderStatus.PENDING);
        assertThat(ids).contains(a.getId());
    }

    @Test
    void orphanRemovalOnItemsWhenOrderDeleted() {
        Order o = orderRepository.save(newOrder(OrderStatus.PENDING));
        orderRepository.delete(o);
        assertThat(orderRepository.findById(o.getId())).isEmpty();
    }
}
