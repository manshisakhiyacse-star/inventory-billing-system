package com.inventory.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private Integer quantityInStock;
    private Integer reorderThreshold;
    private Boolean lowStock;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
