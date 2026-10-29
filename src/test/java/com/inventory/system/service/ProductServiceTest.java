package com.inventory.system.service;

import com.inventory.system.dto.request.ProductRequest;
import com.inventory.system.dto.response.ProductResponse;
import com.inventory.system.entity.Product;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.ProductMapper;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Should successfully create a product and record initial stock")
    void testCreateProductSuccess() {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-100")
                .name("Keyboard")
                .unitPrice(new BigDecimal("49.99"))
                .costPrice(new BigDecimal("25.00"))
                .quantityInStock(20)
                .reorderThreshold(5)
                .build();

        Product product = Product.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Keyboard")
                .quantityInStock(20)
                .build();

        ProductResponse expectedResponse = ProductResponse.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Keyboard")
                .quantityInStock(20)
                .build();

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(expectedResponse);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("SKU-100", response.getSku());
        verify(stockMovementRepository, times(1)).save(any());
        verify(auditService, times(1)).logAction(eq("CREATE_PRODUCT"), eq("Product"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when SKU exists")
    void testCreateProductDuplicateSku() {
        ProductRequest request = ProductRequest.builder().sku("SKU-EXISTS").build();
        when(productRepository.existsBySku("SKU-EXISTS")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown product ID")
    void testGetProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }
}
