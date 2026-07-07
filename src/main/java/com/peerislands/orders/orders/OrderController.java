package com.peerislands.orders.orders;

import com.peerislands.orders.orders.dto.CreateOrderRequest;
import com.peerislands.orders.orders.dto.OrderResponse;
import com.peerislands.orders.orders.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order o = orderService.create(request);
        OrderResponse body = toResponse(o);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + o.getId())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(orderService.get(id)));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(value = "status", required = false) String status) {
        OrderStatus filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalOrderStateException("unknown status: " + status);
            }
        }
        return orderService.findAll(filter).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderStatus target;
        try {
            target = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalOrderStateException("unknown status: " + request.getStatus());
        }
        if (target == OrderStatus.CANCELLED || target == OrderStatus.PENDING) {
            throw new IllegalOrderStateException(
                    "status " + target + " is not allowed via PUT; use POST /cancel or no path");
        }
        Order updated = orderService.transitionTo(id, target);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(orderService.cancel(id)));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderResponse.Item> items = o.getItems().stream()
                .map(i -> new OrderResponse.Item(
                        i.getId(), i.getProduct().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                .collect(Collectors.toList());
        return new OrderResponse(
                o.getId(), o.getCustomerName(), o.getStatus().name(),
                o.getCreatedAt(), o.getUpdatedAt(), o.getVersion(), items);
    }
}
