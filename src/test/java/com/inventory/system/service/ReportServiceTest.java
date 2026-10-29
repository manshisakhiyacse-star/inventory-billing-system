package com.inventory.system.service;

import com.inventory.system.dto.response.InventoryReportResponse;
import com.inventory.system.dto.response.ProductResponse;
import com.inventory.system.dto.response.SalesReportResponse;
import com.inventory.system.entity.Invoice;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.mapper.ProductMapper;
import com.inventory.system.repository.InvoiceRepository;
import com.inventory.system.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Should accurately calculate sales report excluding cancelled invoices")
    void testGetSalesReport() {
        LocalDateTime now = LocalDateTime.now();

        Invoice paidInvoice = Invoice.builder()
                .status(InvoiceStatus.PAID)
                .grandTotal(new BigDecimal("110.00"))
                .taxAmount(new BigDecimal("10.00"))
                .discountAmount(new BigDecimal("0.00"))
                .build();

        Invoice cancelledInvoice = Invoice.builder()
                .status(InvoiceStatus.CANCELLED)
                .grandTotal(new BigDecimal("500.00"))
                .taxAmount(new BigDecimal("50.00"))
                .discountAmount(new BigDecimal("0.00"))
                .build();

        when(invoiceRepository.findByDateRange(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paidInvoice, cancelledInvoice)));

        SalesReportResponse report = reportService.getSalesReport(now.minusDays(7), now);

        assertNotNull(report);
        assertEquals(2, report.getTotalInvoices());
        assertEquals(1, report.getPaidInvoices());
        assertEquals(1, report.getCancelledInvoices());
        assertEquals(new BigDecimal("110.00"), report.getTotalSales(), "Cancelled invoice should not be included in sales");
        assertEquals(new BigDecimal("100.00"), report.getNetRevenue(), "Net revenue = sales - tax");
    }

    @Test
    @DisplayName("Should accurately calculate inventory report valuation")
    void testGetInventoryReport() {
        Product p1 = Product.builder()
                .costPrice(new BigDecimal("10.00"))
                .quantityInStock(5)
                .build(); // 5 * 10 = $50

        Product p2 = Product.builder()
                .costPrice(new BigDecimal("20.00"))
                .quantityInStock(3)
                .build(); // 3 * 20 = $60

        when(productRepository.findByActiveTrue()).thenReturn(List.of(p1, p2));
        when(productRepository.count()).thenReturn(2L);
        when(productRepository.findLowStockProducts(any())).thenReturn(new PageImpl<>(List.of(p1)));
        when(productMapper.toResponseList(List.of(p1))).thenReturn(List.of(new ProductResponse()));

        InventoryReportResponse report = reportService.getInventoryReport();

        assertNotNull(report);
        assertEquals(2L, report.getTotalProducts());
        assertEquals(2L, report.getActiveProducts());
        assertEquals(1L, report.getLowStockProductsCount());
        assertEquals(new BigDecimal("110.00"), report.getTotalInventoryValue());
    }
}
