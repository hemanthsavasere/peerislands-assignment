package com.peerislands.orders.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateProductRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String sku;
    @NotNull @Min(0)
    private Integer availableStock;
    @NotNull @DecimalMin("0.0")
    private BigDecimal unitPrice;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
