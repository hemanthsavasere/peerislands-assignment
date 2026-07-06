package com.peerislands.orders.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductResponse {
    private final UUID id;
    private final String name;
    private final String sku;
    private final BigDecimal unitPrice;
    private final int availableStock;
    private final int reservedStock;
    private final long version;

    public ProductResponse(UUID id, String name, String sku, BigDecimal unitPrice,
                           int availableStock, int reservedStock, long version) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getAvailableStock() { return availableStock; }
    public int getReservedStock() { return reservedStock; }
    public long getVersion() { return version; }
}
