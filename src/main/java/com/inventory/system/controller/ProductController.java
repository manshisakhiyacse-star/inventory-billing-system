package com.inventory.system.controller;

import com.inventory.system.dto.request.ProductRequest;
import com.inventory.system.dto.response.ProductResponse;
import com.inventory.system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Endpoints for managing products and catalog inventory")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create product", description = "Admin or Manager only. Adds a new product to the catalog.")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update product", description = "Admin or Manager only. Updates an existing product.")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns details for a specific product.")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "List products", description = "Returns paginated active products with optional keyword search.")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(search, pageable));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CLERK')")
    @Operation(summary = "List low-stock products", description = "Returns products whose stock level is at or below their reorder threshold.")
    public ResponseEntity<Page<ProductResponse>> getLowStockProducts(
            @PageableDefault(size = 20, sort = "quantityInStock", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getLowStockProducts(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate product", description = "Admin only. Soft deletes/deactivates a product from active inventory.")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivateProduct(id));
    }
}
