package com.chirag.shopsphere.dto;

import com.chirag.shopsphere.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal cartTotal;

    public static CartResponse of(Cart cart, List<CartItemResponse> items) {
        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(items)
                .totalItems(totalQuantity)
                .cartTotal(total)
                .build();
    }
}
