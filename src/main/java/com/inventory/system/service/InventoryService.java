package com.inventory.system.service;

import com.inventory.system.dto.request.StockAdjustmentRequest;
import com.inventory.system.dto.response.StockMovementResponse;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.StockMovement;
import com.inventory.system.entity.enums.StockMovementType;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.StockMovementMapper;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementMapper stockMovementMapper;
    private final AuditService auditService;

    @Transactional
    public StockMovementResponse adjustStock(StockAdjustmentRequest request) {
        // Acquire pessimistic write lock (SELECT ... FOR UPDATE) to prevent concurrency races
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        int currentStock = product.getQuantityInStock();
        int newStock = currentStock + request.getQuantityChange();

        if (newStock < 0) {
            throw new InsufficientStockException(
                    product.getId(),
                    product.getSku(),
                    Math.abs(request.getQuantityChange()),
                    currentStock
            );
        }

        product.setQuantityInStock(newStock);
        productRepository.save(product);

        String username = getCurrentUsername();
        StockMovementType movementType = request.getQuantityChange() >= 0
                ? StockMovementType.PURCHASE
                : StockMovementType.ADJUSTMENT;

        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantityChange(request.getQuantityChange())
                .quantityAfter(newStock)
                .reason(request.getReason())
                .performedBy(username)
                .build();

        StockMovement savedMovement = stockMovementRepository.save(movement);

        auditService.logAction(
                "ADJUST_STOCK",
                "Product",
                product.getId(),
                String.format("Adjusted stock by %d (from %d to %d). Reason: %s",
                        request.getQuantityChange(), currentStock, newStock, request.getReason()),
                username
        );

        return stockMovementMapper.toResponse(savedMovement);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getStockHistory(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getAllStockMovements(Pageable pageable) {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(stockMovementMapper::toResponse);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
