package com.inventory.system.controller;

import com.inventory.system.dto.response.InventoryReportResponse;
import com.inventory.system.dto.response.SalesReportResponse;
import com.inventory.system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Analytics & Reports", description = "Admin and Manager endpoints for sales summaries and inventory valuation")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @Operation(summary = "Get sales report", description = "Aggregates revenue, invoices count, discounts, and taxes within an optional date range.")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(reportService.getSalesReport(startDate, endDate));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get inventory valuation report", description = "Returns total catalog valuation, total active items, and details on low-stock items.")
    public ResponseEntity<InventoryReportResponse> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport());
    }
}
