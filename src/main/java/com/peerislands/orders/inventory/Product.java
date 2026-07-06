package com.peerislands.orders.inventory;

import com.peerislands.orders.common.InsufficientStockException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;
    private String name;
    private String sku;
    private BigDecimal unitPrice;
    private int availableStock;
    private int reservedStock;

    @Version
    private long version;

    protected Product() {
    }

    public Product(UUID id, String name, String sku, BigDecimal unitPrice, int availableStock, int reservedStock) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.version = 0L;
        validateStocks();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getAvailableStock() { return availableStock; }
    public int getReservedStock() { return reservedStock; }
    public long getVersion() { return version; }

    public void setName(String name) { this.name = name; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public void setAvailableStock(int availableStock) {
        if (availableStock < 0) {
            throw new IllegalArgumentException("availableStock must be >= 0");
        }
        this.availableStock = availableStock;
    }

    public void setReservedStock(int reservedStock) {
        if (reservedStock < 0) {
            throw new IllegalArgumentException("reservedStock must be >= 0");
        }
        this.reservedStock = reservedStock;
    }

    public void reserve(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("reserve qty must be > 0");
        }
        if (qty > availableStock) {
            throw new InsufficientStockException(
                    "insufficient stock for sku " + sku + ": have " + availableStock + ", want " + qty);
        }
        this.availableStock -= qty;
        this.reservedStock += qty;
    }

    public void release(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("release qty must be > 0");
        }
        if (qty > this.reservedStock) {
            throw new IllegalStateException(
                    "cannot release " + qty + " when reservedStock=" + this.reservedStock);
        }
        this.availableStock += qty;
        this.reservedStock -= qty;
    }

    public void finalizeReservation(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("finalizeReservation qty must be > 0");
        }
        if (qty > this.reservedStock) {
            throw new IllegalStateException(
                    "cannot finalize " + qty + " when reservedStock=" + this.reservedStock);
        }
        this.reservedStock -= qty;
    }

    private void validateStocks() {
        if (availableStock < 0 || reservedStock < 0) {
            throw new IllegalArgumentException("stock values must be non-negative");
        }
    }
}
