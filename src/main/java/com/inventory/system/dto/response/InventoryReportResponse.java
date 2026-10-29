package com.inventory.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportResponse {

    private Long totalProducts;
    private Long activeProducts;
    private Long lowStockProductsCount;
    private BigDecimal totalInventoryValue;
    private List<ProductResponse> lowStockProducts;
}
