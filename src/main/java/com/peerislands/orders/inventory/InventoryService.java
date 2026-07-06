package com.peerislands.orders.inventory;

import com.peerislands.orders.inventory.dto.CreateProductRequest;
import com.peerislands.orders.inventory.dto.UpdateProductRequest;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    Product create(CreateProductRequest request);
    Product get(UUID id);
    List<Product> findAll();
    Product update(UUID id, UpdateProductRequest request);
    void delete(UUID id);
    Product findBySku(String sku);
    void reserve(String sku, int qty);
    void release(String sku, int qty);
    void finalizeReservation(String sku, int qty);
}
