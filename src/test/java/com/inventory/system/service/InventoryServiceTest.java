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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private StockMovementMapper stockMovementMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("Should successfully increase stock and record movement")
    void testAdjustStockIncrease() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(1L, 15, "New batch received");

        Product product = Product.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Keyboard")
                .quantityInStock(10)
                .build();

        StockMovement savedMovement = StockMovement.builder()
                .id(100L)
                .product(product)
                .movementType(StockMovementType.PURCHASE)
                .quantityChange(15)
                .quantityAfter(25)
                .reason("New batch received")
                .build();

        StockMovementResponse responseDto = StockMovementResponse.builder()
                .id(100L)
                .productSku("SKU-100")
                .quantityChange(15)
                .quantityAfter(25)
                .build();

        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(savedMovement);
        when(stockMovementMapper.toResponse(savedMovement)).thenReturn(responseDto);

        StockMovementResponse result = inventoryService.adjustStock(request);

        assertNotNull(result);
        assertEquals(25, product.getQuantityInStock());
        verify(productRepository).findByIdWithLock(1L);
        verify(auditService).logAction(eq("ADJUST_STOCK"), eq("Product"), eq(1L), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock reduction exceeds available quantity")
    void testAdjustStockInsufficient() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(1L, -50, "Damage deduction");

        Product product = Product.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Keyboard")
                .quantityInStock(20)
                .build();

        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.adjustStock(request)
        );

        assertEquals(50, ex.getRequested());
        assertEquals(20, ex.getAvailable());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void testAdjustStockProductNotFound() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(999L, 10, "Restock");
        when(productRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.adjustStock(request));
    }
}
