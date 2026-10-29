package com.inventory.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItemResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
