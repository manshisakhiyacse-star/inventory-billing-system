package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.request.ProductRequest;
import com.inventory.system.dto.response.ProductResponse;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.GlobalExceptionHandler;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/products creates product and returns 201 Created")
    void testCreateProduct() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-100")
                .name("Keyboard")
                .unitPrice(new BigDecimal("49.99"))
                .costPrice(new BigDecimal("25.00"))
                .quantityInStock(15)
                .reorderThreshold(5)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Keyboard")
                .unitPrice(new BigDecimal("49.99"))
                .quantityInStock(15)
                .build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    @DisplayName("POST /api/v1/products returns 409 Conflict when SKU already exists")
    void testCreateProductConflict() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-DUP")
                .name("Mouse")
                .unitPrice(new BigDecimal("19.99"))
                .costPrice(new BigDecimal("10.00"))
                .quantityInStock(10)
                .reorderThreshold(2)
                .build();

        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Product", "sku", "SKU-DUP"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 404 when product is not found")
    void testGetProductNotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/products returns paginated products")
    void testGetAllProducts() throws Exception {
        ProductResponse p = ProductResponse.builder().id(1L).sku("SKU-A").name("Item A").build();
        when(productService.getAllProducts(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-A"));
    }
}
