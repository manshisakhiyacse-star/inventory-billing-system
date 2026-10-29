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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<Invoice> invoices = invoiceRepository.findByDateRange(start, end, Pageable.unpaged()).getContent();

        long totalInvoices = invoices.size();
        long paidInvoices = 0;
        long cancelledInvoices = 0;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                paidInvoices++;
            } else if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOIDED) {
                cancelledInvoices++;
            }

            // Exclude cancelled invoices from financial revenue
            if (invoice.getStatus() != InvoiceStatus.CANCELLED && invoice.getStatus() != InvoiceStatus.VOIDED) {
                totalSales = totalSales.add(invoice.getGrandTotal());
                totalDiscount = totalDiscount.add(invoice.getDiscountAmount());
                totalTax = totalTax.add(invoice.getTaxAmount());
            }
        }

        BigDecimal netRevenue = totalSales.subtract(totalTax).setScale(2, RoundingMode.HALF_UP);

        return SalesReportResponse.builder()
                .startDate(start)
                .endDate(end)
                .totalInvoices(totalInvoices)
                .paidInvoices(paidInvoices)
                .cancelledInvoices(cancelledInvoices)
                .totalSales(totalSales.setScale(2, RoundingMode.HALF_UP))
                .totalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .totalTax(totalTax.setScale(2, RoundingMode.HALF_UP))
                .netRevenue(netRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport() {
        List<Product> activeProducts = productRepository.findByActiveTrue();
        long totalProducts = productRepository.count();
        long activeProductsCount = activeProducts.size();

        List<Product> lowStockEntities = productRepository.findLowStockProducts(Pageable.unpaged()).getContent();
        List<ProductResponse> lowStockResponses = productMapper.toResponseList(lowStockEntities);

        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        for (Product product : activeProducts) {
            if (product.getCostPrice() != null && product.getQuantityInStock() != null) {
                BigDecimal value = product.getCostPrice()
                        .multiply(BigDecimal.valueOf(product.getQuantityInStock()));
                totalInventoryValue = totalInventoryValue.add(value);
            }
        }

        return InventoryReportResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProductsCount)
                .lowStockProductsCount((long) lowStockResponses.size())
                .totalInventoryValue(totalInventoryValue.setScale(2, RoundingMode.HALF_UP))
                .lowStockProducts(lowStockResponses)
                .build();
    }
}
