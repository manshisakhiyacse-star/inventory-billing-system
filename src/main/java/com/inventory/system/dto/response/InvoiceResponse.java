package com.inventory.system.dto.response;

import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.entity.enums.PaymentMethod;
import com.inventory.system.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private InvoiceStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String notes;
    private String createdBy;
    private Boolean stockRestored;
    private List<InvoiceLineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
