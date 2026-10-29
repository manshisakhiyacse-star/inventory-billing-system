package com.inventory.system.controller;

import com.inventory.system.dto.response.InventoryReportResponse;
import com.inventory.system.dto.response.SalesReportResponse;
import com.inventory.system.exception.InvalidOperationException;
import com.inventory.system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Analytics & Reports", description = "Admin and Manager endpoints for sales summaries and inventory valuation")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @Operation(
            summary = "Get sales report",
            description = "Aggregates revenue, invoices count, discounts, and taxes within an optional date range. " +
                          "Accepts dates ('YYYY-MM-DD') or date-times ('YYYY-MM-DDTHH:mm:ss'). Defaults to the last 30 days if omitted."
    )
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @Parameter(description = "Start date (e.g. '2026-10-01' or '2026-10-01T00:00:00')", example = "2026-10-01")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date (e.g. '2026-10-29' or '2026-10-29T23:59:59')", example = "2026-10-29")
            @RequestParam(required = false) String endDate) {

        LocalDateTime start = parseDateTime(startDate, true);
        LocalDateTime end = parseDateTime(endDate, false);

        return ResponseEntity.ok(reportService.getSalesReport(start, end));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get inventory valuation report", description = "Returns total catalog valuation, total active items, and details on low-stock items.")
    public ResponseEntity<InventoryReportResponse> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport());
    }

    private LocalDateTime parseDateTime(String input, boolean isStart) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String trimmed = input.trim();
        try {
            if (trimmed.contains("T")) {
                // If it ends with 'Z' or offset, strip or parse
                if (trimmed.endsWith("Z")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                return LocalDateTime.parse(trimmed);
            } else {
                LocalDate date = LocalDate.parse(trimmed);
                return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            }
        } catch (DateTimeParseException ex) {
            throw new InvalidOperationException(
                    "Invalid date format for '" + input + "'. Please use 'YYYY-MM-DD' (e.g. 2026-10-01) or 'YYYY-MM-DDTHH:mm:ss' (e.g. 2026-10-01T00:00:00)."
            );
        }
    }
}
