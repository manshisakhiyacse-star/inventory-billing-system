package com.inventory.system.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price must have at most 10 integer and 2 fractional digits")
    private BigDecimal unitPrice;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", message = "Cost price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Cost price must have at most 10 integer and 2 fractional digits")
    private BigDecimal costPrice;

    @NotNull(message = "Quantity in stock is required")
    @Min(value = 0, message = "Quantity in stock must be non-negative")
    private Integer quantityInStock;

    @NotNull(message = "Reorder threshold is required")
    @Min(value = 0, message = "Reorder threshold must be non-negative")
    private Integer reorderThreshold;
}
