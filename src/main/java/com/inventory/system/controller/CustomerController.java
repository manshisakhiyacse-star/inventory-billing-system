package com.inventory.system.controller;

import com.inventory.system.dto.request.CustomerRequest;
import com.inventory.system.dto.response.CustomerResponse;
import com.inventory.system.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoints for managing customer records and contacts")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BILLING_CLERK')")
    @Operation(summary = "Create customer", description = "Adds a new customer for billing and invoicing.")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BILLING_CLERK')")
    @Operation(summary = "Update customer", description = "Updates details of an existing customer.")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer information by customer ID.")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping
    @Operation(summary = "List customers", description = "Returns paginated active customers with optional search across name, email, and phone.")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(search, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Deactivate customer", description = "Admin or Manager only. Deactivates a customer record.")
    public ResponseEntity<CustomerResponse> deactivateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.deactivateCustomer(id));
    }
}
