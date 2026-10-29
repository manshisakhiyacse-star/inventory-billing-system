package com.inventory.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
