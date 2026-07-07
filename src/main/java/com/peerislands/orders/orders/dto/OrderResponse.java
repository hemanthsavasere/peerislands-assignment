package com.peerislands.orders.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderResponse {
    private final UUID id;
    private final String customerName;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private final List<Item> items;

    public OrderResponse(UUID id, String customerName, String status, Instant createdAt, Instant updatedAt,
                          long version, List<Item> items) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<Item> getItems() { return items; }

    public static class Item {
        private final UUID id;
        private final UUID productId;
        private final String productName;
        private final BigDecimal unitPrice;
        private final int quantity;

        public Item(UUID id, UUID productId, String productName, BigDecimal unitPrice, int quantity) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public UUID getId() { return id; }
        public UUID getProductId() { return productId; }
        public String getProductName() { return productName; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public int getQuantity() { return quantity; }
    }
}
