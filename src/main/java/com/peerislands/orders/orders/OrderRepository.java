package com.peerislands.orders.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStatus(OrderStatus status);

    @Query("select o.id from Order o where o.status = :status")
    List<UUID> findIdsByStatus(@Param("status") OrderStatus status);
}
