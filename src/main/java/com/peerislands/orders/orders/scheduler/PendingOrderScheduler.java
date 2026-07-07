package com.peerislands.orders.orders.scheduler;

import com.peerislands.orders.orders.OrderStatus;
import com.peerislands.orders.orders.OrderService;
import com.peerislands.orders.orders.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PendingOrderScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingOrderScheduler.class);

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public PendingOrderScheduler(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "${orders.scheduler.interval-ms:300000}")
    public void advancePendingOrders() {
        long t0 = System.currentTimeMillis();
        List<UUID> ids = orderRepository.findIdsByStatus(OrderStatus.PENDING);
        int advanced = 0;
        for (UUID id : ids) {
            try {
                orderService.transitionTo(id, OrderStatus.PROCESSING);
                advanced++;
            } catch (Exception e) {
                log.warn("scheduler: skip order {} ({})", id, e.getMessage());
            }
        }
        log.info("scheduler: advanced {} pending orders in {}ms", advanced, System.currentTimeMillis() - t0);
    }
}
