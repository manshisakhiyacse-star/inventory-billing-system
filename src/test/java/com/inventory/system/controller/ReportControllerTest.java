package com.inventory.system.controller;

import com.inventory.system.dto.response.SalesReportResponse;
import com.inventory.system.exception.GlobalExceptionHandler;
import com.inventory.system.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/reports/sales works with simple date format (YYYY-MM-DD)")
    void testGetSalesReportWithDateOnly() throws Exception {
        SalesReportResponse response = SalesReportResponse.builder()
                .totalSales(new BigDecimal("1000.00"))
                .netRevenue(new BigDecimal("900.00"))
                .build();

        when(reportService.getSalesReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/sales?startDate=2026-10-01&endDate=2026-10-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(1000.00))
                .andExpect(jsonPath("$.netRevenue").value(900.00));

        verify(reportService).getSalesReport(
                LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                LocalDateTime.of(2026, 10, 29, 23, 59, 59, 999999999)
        );
    }

    @Test
    @DisplayName("GET /api/v1/reports/sales works with full ISO date-time format")
    void testGetSalesReportWithDateTime() throws Exception {
        SalesReportResponse response = SalesReportResponse.builder()
                .totalSales(new BigDecimal("500.00"))
                .build();

        when(reportService.getSalesReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/sales?startDate=2026-10-01T08:00:00&endDate=2026-10-29T18:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(500.00));

        verify(reportService).getSalesReport(
                LocalDateTime.of(2026, 10, 1, 8, 0, 0),
                LocalDateTime.of(2026, 10, 29, 18, 0, 0)
        );
    }

    @Test
    @DisplayName("GET /api/v1/reports/sales returns 400 for invalid date string")
    void testGetSalesReportInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/reports/sales?startDate=not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Operation"));
    }
}
