package com.inventory.system.dto.response;

import com.inventory.system.entity.enums.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private StockMovementType movementType;
    private Integer quantityChange;
    private Integer quantityAfter;
    private Long referenceId;
    private String reason;
    private String performedBy;
    private LocalDateTime createdAt;
}
