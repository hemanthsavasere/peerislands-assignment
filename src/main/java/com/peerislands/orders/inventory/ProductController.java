package com.peerislands.orders.inventory;

import com.peerislands.orders.inventory.dto.CreateProductRequest;
import com.peerislands.orders.inventory.dto.ProductResponse;
import com.peerislands.orders.inventory.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product p = inventoryService.create(request);
        ProductResponse body = toResponse(p);
        return ResponseEntity.created(URI.create("/api/v1/products/" + p.getId())).body(body);
    }

    @GetMapping
    public List<ProductResponse> list() {
        return inventoryService.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(inventoryService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(toResponse(inventoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getSku(), p.getUnitPrice(),
                p.getAvailableStock(), p.getReservedStock(), p.getVersion());
    }
}
