package com.inventory.system.dto.request;

import com.inventory.system.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class InvoiceRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotEmpty(message = "Invoice must contain at least one line item")
    @Valid
    private List<InvoiceLineItemRequest> lineItems;

    @DecimalMin(value = "0.00", message = "Discount amount must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Discount amount must have at most 10 integer and 2 fractional digits")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0000", message = "Tax rate must be non-negative")
    @DecimalMax(value = "1.0000", message = "Tax rate cannot exceed 1.0000 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Tax rate must have at most 1 integer and 4 fractional digits")
    private BigDecimal taxRate;

    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
