package com.chirag.shopsphere.dto;

import com.chirag.shopsphere.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @NotNull(message = "Transaction type is required (RESTOCK, ADJUSTMENT, etc.)")
    private TransactionType type;

    private String reference;
}
