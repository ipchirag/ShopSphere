package com.chirag.shopsphere.controller;

import com.chirag.shopsphere.dto.InventoryTransactionResponse;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.dto.StockAdjustmentRequest;
import com.chirag.shopsphere.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> listTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(inventoryService.listTransactions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryTransactionResponse> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getTransactionById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> listTransactionsByProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(inventoryService.listTransactionsByProduct(productId, pageable));
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryTransactionResponse> recordStockAdjustment(
            @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryTransactionResponse response = inventoryService.recordStockAdjustment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
