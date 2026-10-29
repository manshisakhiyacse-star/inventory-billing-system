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
public class SalesReportResponse {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalInvoices;
    private Long paidInvoices;
    private Long cancelledInvoices;
    private BigDecimal totalSales;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal netRevenue;
}
