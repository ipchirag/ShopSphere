package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.AddToCartRequest;
import com.chirag.shopsphere.dto.CartItemResponse;
import com.chirag.shopsphere.dto.CartResponse;
import com.chirag.shopsphere.dto.UpdateCartItemRequest;
import com.chirag.shopsphere.entity.Cart;
import com.chirag.shopsphere.entity.CartItem;
import com.chirag.shopsphere.entity.Product;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.exception.BadRequestException;
import com.chirag.shopsphere.exception.InsufficientStockException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.CartItemRepository;
import com.chirag.shopsphere.repository.CartRepository;
import com.chirag.shopsphere.repository.ProductRepository;
import com.chirag.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product '" + product.getName() + "' is currently inactive");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        int newTotalQuantity = request.getQuantity();
        CartItem cartItem;

        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            newTotalQuantity = cartItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newTotalQuantity) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), newTotalQuantity);
            }
            cartItem.setQuantity(newTotalQuantity);
        } else {
            if (product.getStockQuantity() < newTotalQuantity) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), newTotalQuantity);
            }
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(newTotalQuantity)
                    .build();
        }

        cartItemRepository.save(cartItem);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to the user's cart");
        }

        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), request.getQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to the user's cart");
        }

        cartItemRepository.delete(cartItem);
        return buildCartResponse(cart);
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = items.stream()
                .map(CartItemResponse::fromEntity)
                .toList();
        return CartResponse.of(cart, itemResponses);
    }
}
