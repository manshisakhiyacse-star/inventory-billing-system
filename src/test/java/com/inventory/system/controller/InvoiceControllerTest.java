package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.request.InvoiceLineItemRequest;
import com.inventory.system.dto.request.InvoiceRequest;
import com.inventory.system.dto.response.InvoiceResponse;
import com.inventory.system.exception.GlobalExceptionHandler;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/invoices creates invoice and returns 201 Created")
    void testCreateInvoice() throws Exception {
        InvoiceRequest request = InvoiceRequest.builder()
                .customerId(1L)
                .lineItems(List.of(new InvoiceLineItemRequest(10L, 2)))
                .build();

        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20261029-0001")
                .grandTotal(new BigDecimal("40.00"))
                .build();

        when(invoiceService.createInvoice(any(InvoiceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-20261029-0001"))
                .andExpect(jsonPath("$.grandTotal").value(40.00));
    }

    @Test
    @DisplayName("POST /api/v1/invoices returns 400 when insufficient stock")
    void testCreateInvoiceInsufficientStock() throws Exception {
        InvoiceRequest request = InvoiceRequest.builder()
                .customerId(1L)
                .lineItems(List.of(new InvoiceLineItemRequest(10L, 50)))
                .build();

        when(invoiceService.createInvoice(any(InvoiceRequest.class)))
                .thenThrow(new InsufficientStockException(10L, "SKU-A", 50, 5));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"));
    }

    @Test
    @DisplayName("PATCH /api/v1/invoices/{id}/cancel returns 200 with cancelled status")
    void testCancelInvoice() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20261029-0001")
                .stockRestored(true)
                .build();

        when(invoiceService.cancelInvoice(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/invoices/1/cancel?reason=Returned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockRestored").value(true));
    }
}
