package com.chirag.shopsphere.dto;

import com.chirag.shopsphere.entity.InventoryTransaction;
import com.chirag.shopsphere.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Integer quantityChange;
    private TransactionType type;
    private String reference;
    private LocalDateTime createdAt;

    public static InventoryTransactionResponse fromEntity(InventoryTransaction tx) {
        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .productId(tx.getProduct() != null ? tx.getProduct().getId() : null)
                .productName(tx.getProduct() != null ? tx.getProduct().getName() : null)
                .quantityChange(tx.getQuantityChange())
                .type(tx.getType())
                .reference(tx.getReference())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
