package com.inventory.system.controller;

import com.inventory.system.dto.request.StockAdjustmentRequest;
import com.inventory.system.dto.response.StockMovementResponse;
import com.inventory.system.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints for inventory adjustments and stock audit history")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CLERK')")
    @Operation(summary = "Adjust stock quantity", description = "Applies atomic positive/negative stock adjustment with pessimistic write lock.")
    public ResponseEntity<StockMovementResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        StockMovementResponse response = inventoryService.adjustStock(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{productId}")
    @Operation(summary = "Get stock history for a product", description = "Returns chronological stock movement history for a given product.")
    public ResponseEntity<Page<StockMovementResponse>> getStockHistory(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getStockHistory(productId, pageable));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CLERK')")
    @Operation(summary = "List all stock movements", description = "Returns global chronological stock movements across all products.")
    public ResponseEntity<Page<StockMovementResponse>> getAllStockMovements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllStockMovements(pageable));
    }
}
