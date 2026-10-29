package com.inventory.system.controller;

import com.inventory.system.dto.request.InvoiceRequest;
import com.inventory.system.dto.response.InvoiceResponse;
import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Billing & Invoicing", description = "Endpoints for creating invoices, processing payments, and cancellation")
@SecurityRequirement(name = "Bearer Authentication")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BILLING_CLERK')")
    @Operation(summary = "Generate new invoice", description = "Creates invoice, decrements inventory atomically with pessimistic lock, and records stock movements.")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves an invoice including its full line item breakdown.")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/number/{invoiceNumber}")
    @Operation(summary = "Get invoice by invoice number", description = "Retrieves an invoice by its unique human-readable invoice number.")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping
    @Operation(summary = "List invoices", description = "Returns paginated invoices with optional filters for status and customer ID.")
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(status, customerId, pageable));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BILLING_CLERK')")
    @Operation(summary = "Cancel invoice", description = "Cancels an invoice and restores inventory idempotently.")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Customer requested cancellation") String reason) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id, reason));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BILLING_CLERK')")
    @Operation(summary = "Mark invoice as paid", description = "Updates invoice status to PAID and payment status to COMPLETED.")
    public ResponseEntity<InvoiceResponse> markInvoicePaid(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.markInvoicePaid(id));
    }
}
