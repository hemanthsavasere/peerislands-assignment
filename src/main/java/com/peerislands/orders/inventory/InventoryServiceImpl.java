package com.peerislands.orders.inventory;

import com.peerislands.orders.common.ActiveReservationException;
import com.peerislands.orders.common.DuplicateSkuException;
import com.peerislands.orders.common.ResourceNotFoundException;
import com.peerislands.orders.inventory.dto.CreateProductRequest;
import com.peerislands.orders.inventory.dto.UpdateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final ReservationLookup reservationLookup;

    public InventoryServiceImpl(ProductRepository productRepository, ReservationLookup reservationLookup) {
        this.productRepository = productRepository;
        this.reservationLookup = reservationLookup;
    }

    @Override
    @Transactional
    public Product create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException("product with sku " + request.getSku() + " already exists");
        }
        Product product = new Product(
                UUID.randomUUID(),
                request.getName(),
                request.getSku(),
                request.getUnitPrice(),
                request.getAvailableStock(),
                0);
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product get(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Product findBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("product with sku " + sku + " not found"));
    }

    @Override
    @Transactional
    public Product update(UUID id, UpdateProductRequest request) {
        Product p = get(id);
        p.setName(request.getName());
        p.setAvailableStock(request.getAvailableStock());
        p.setUnitPrice(request.getUnitPrice());
        return productRepository.save(p);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product p = get(id);
        if (reservationLookup.hasActiveReservation(id)) {
            throw new ActiveReservationException(
                    "product " + id + " has active reservations and cannot be deleted");
        }
        productRepository.delete(p);
    }

    @Override
    @Transactional
    public void reserve(String sku, int qty) {
        Product p = findBySku(sku);
        p.reserve(qty);
        productRepository.save(p);
    }

    @Override
    @Transactional
    public void release(String sku, int qty) {
        Product p = findBySku(sku);
        p.release(qty);
        productRepository.save(p);
    }

    @Override
    @Transactional
    public void finalizeReservation(String sku, int qty) {
        Product p = findBySku(sku);
        p.finalizeReservation(qty);
        productRepository.save(p);
    }
}
