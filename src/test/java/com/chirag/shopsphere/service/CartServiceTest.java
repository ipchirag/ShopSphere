package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.AddToCartRequest;
import com.chirag.shopsphere.dto.CartResponse;
import com.chirag.shopsphere.entity.Cart;
import com.chirag.shopsphere.entity.Product;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.entity.UserRole;
import com.chirag.shopsphere.exception.BadRequestException;
import com.chirag.shopsphere.exception.InsufficientStockException;
import com.chirag.shopsphere.repository.CartItemRepository;
import com.chirag.shopsphere.repository.CartRepository;
import com.chirag.shopsphere.repository.ProductRepository;
import com.chirag.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User sampleUser;
    private Cart sampleCart;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Alice")
                .email("alice@example.com")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        sampleCart = Cart.builder()
                .id(10L)
                .user(sampleUser)
                .build();

        sampleProduct = Product.builder()
                .id(20L)
                .name("Keyboard")
                .sku("KEY-01")
                .price(new BigDecimal("49.99"))
                .stockQuantity(10)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Add product to cart successfully when stock is available")
    void shouldAddProductToCartSuccessfully() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));
        when(cartItemRepository.findByCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(20L)
                .quantity(3)
                .build();

        CartResponse response = cartService.addToCart(1L, request);

        assertNotNull(response);
        verify(cartItemRepository).save(any());
    }

    @Test
    @DisplayName("Fail to add to cart when product is inactive")
    void shouldFailWhenProductIsInactive() {
        sampleProduct.setActive(false);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(20L)
                .quantity(1)
                .build();

        assertThrows(BadRequestException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    @DisplayName("Fail to add to cart when quantity exceeds available stock")
    void shouldFailWhenQuantityExceedsStock() {
        sampleProduct.setStockQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));
        when(cartItemRepository.findByCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(20L)
                .quantity(5)
                .build();

        assertThrows(InsufficientStockException.class, () -> cartService.addToCart(1L, request));
    }
}
