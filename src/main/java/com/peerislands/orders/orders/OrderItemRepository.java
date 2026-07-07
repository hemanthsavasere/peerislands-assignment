package com.peerislands.orders.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("select count(oi) > 0 from OrderItem oi " +
           "join oi.order o " +
           "where oi.product.id = :productId and o.status in :statuses")
    boolean existsByProductIdAndOrderStatusIn(@Param("productId") UUID productId,
                                              @Param("statuses") List<OrderStatus> statuses);
}
