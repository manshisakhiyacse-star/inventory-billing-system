package com.inventory.system.service;

import com.inventory.system.dto.request.ProductRequest;
import com.inventory.system.dto.response.ProductResponse;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.StockMovement;
import com.inventory.system.entity.enums.StockMovementType;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.ProductMapper;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;
    private final AuditService auditService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Product product = productMapper.toEntity(request);
        product.setActive(true);
        Product savedProduct = productRepository.save(product);

        // Record initial stock movement if starting with positive inventory
        if (savedProduct.getQuantityInStock() > 0) {
            StockMovement movement = StockMovement.builder()
                    .product(savedProduct)
                    .movementType(StockMovementType.PURCHASE)
                    .quantityChange(savedProduct.getQuantityInStock())
                    .quantityAfter(savedProduct.getQuantityInStock())
                    .reason("Initial stock on product creation")
                    .performedBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);
        }

        auditService.logAction("CREATE_PRODUCT", "Product", savedProduct.getId(),
                "Created product: " + savedProduct.getName() + " (SKU: " + savedProduct.getSku() + ")");

        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // If SKU changed, verify uniqueness
        if (!product.getSku().equalsIgnoreCase(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        productMapper.updateEntityFromRequest(request, product);
        Product updatedProduct = productRepository.save(product);

        auditService.logAction("UPDATE_PRODUCT", "Product", updatedProduct.getId(),
                "Updated product: " + updatedProduct.getName() + " (SKU: " + updatedProduct.getSku() + ")");

        return productMapper.toResponse(updatedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String search, Pageable pageable) {
        Page<Product> page;
        if (search != null && !search.trim().isEmpty()) {
            page = productRepository.searchByKeyword(search.trim(), pageable);
        } else {
            page = productRepository.findByActiveTrue(pageable);
        }
        return page.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getLowStockProducts(Pageable pageable) {
        return productRepository.findLowStockProducts(pageable).map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(false);
        Product updated = productRepository.save(product);

        auditService.logAction("DEACTIVATE_PRODUCT", "Product", updated.getId(),
                "Deactivated product: " + updated.getSku());

        return productMapper.toResponse(updated);
    }
}
