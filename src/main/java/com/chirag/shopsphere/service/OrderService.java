package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.CreateOrderRequest;
import com.chirag.shopsphere.dto.OrderItemResponse;
import com.chirag.shopsphere.dto.OrderResponse;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.dto.UpdateOrderStatusRequest;
import com.chirag.shopsphere.entity.Cart;
import com.chirag.shopsphere.entity.CartItem;
import com.chirag.shopsphere.entity.Coupon;
import com.chirag.shopsphere.entity.DiscountType;
import com.chirag.shopsphere.entity.InventoryTransaction;
import com.chirag.shopsphere.entity.Order;
import com.chirag.shopsphere.entity.OrderItem;
import com.chirag.shopsphere.entity.OrderStatus;
import com.chirag.shopsphere.entity.Product;
import com.chirag.shopsphere.entity.TransactionType;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.exception.BadRequestException;
import com.chirag.shopsphere.exception.InsufficientStockException;
import com.chirag.shopsphere.exception.InvalidStatusTransitionException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.CartItemRepository;
import com.chirag.shopsphere.repository.CartRepository;
import com.chirag.shopsphere.repository.CouponRepository;
import com.chirag.shopsphere.repository.InventoryTransactionRepository;
import com.chirag.shopsphere.repository.OrderItemRepository;
import com.chirag.shopsphere.repository.OrderRepository;
import com.chirag.shopsphere.repository.ProductRepository;
import com.chirag.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("User has no active cart to place an order"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot create order from an empty cart");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            // Pessimistic write lock prevents race conditions and overselling across concurrent transactions
            Product lockedProduct = productRepository.findByIdWithLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProduct().getId()));

            if (!lockedProduct.isActive()) {
                throw new BadRequestException("Product '" + lockedProduct.getName() + "' is inactive and cannot be ordered");
            }

            if (lockedProduct.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(lockedProduct.getName(), lockedProduct.getStockQuantity(), cartItem.getQuantity());
            }

            // Deduct stock safely
            lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(lockedProduct);

            // Record inventory change in audit ledger
            InventoryTransaction invTx = InventoryTransaction.builder()
                    .product(lockedProduct)
                    .quantityChange(-cartItem.getQuantity())
                    .type(TransactionType.ORDER)
                    .reference("Order " + orderNumber)
                    .build();
            inventoryTransactionRepository.save(invTx);

            BigDecimal lineTotal = lockedProduct.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(lockedProduct)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(lockedProduct.getPrice())
                    .lineTotal(lineTotal)
                    .build();

            orderItemsToSave.add(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discountAmount = calculateCouponDiscount(request.getCouponCode().trim(), subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItemsToSave) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        // Clear cart to avoid accidental double-ordering
        cartItemRepository.deleteByCartId(cart.getId());

        List<OrderItemResponse> itemResponses = orderItemsToSave.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        return OrderResponse.of(savedOrder, itemResponses);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        return OrderResponse.of(order, items);
    }

    public PageResponse<OrderResponse> listUserOrders(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        Page<OrderResponse> page = orderRepository.findByUserId(userId, pageable)
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                            .map(OrderItemResponse::fromEntity)
                            .toList();
                    return OrderResponse.of(order, items);
                });
        return PageResponse.from(page);
    }

    public PageResponse<OrderResponse> listAllOrders(Pageable pageable) {
        Page<OrderResponse> page = orderRepository.findAll(pageable)
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                            .map(OrderItemResponse::fromEntity)
                            .toList();
                    return OrderResponse.of(order, items);
                });
        return PageResponse.from(page);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateStatusTransition(order.getStatus(), request.getStatus());
        order.setStatus(request.getStatus());

        Order updated = orderRepository.save(order);
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(updated.getId()).stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        return OrderResponse.of(updated, items);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel order with status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        // Restock products and log inventory adjustment
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Product lockedProduct = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProduct().getId()));

            lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() + item.getQuantity());
            productRepository.save(lockedProduct);

            InventoryTransaction tx = InventoryTransaction.builder()
                    .product(lockedProduct)
                    .quantityChange(item.getQuantity())
                    .type(TransactionType.CANCELLATION)
                    .reference("Cancelled Order " + order.getOrderNumber())
                    .build();
            inventoryTransactionRepository.save(tx);
        }

        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        return OrderResponse.of(updatedOrder, itemResponses);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return;
        }

        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(current.name(), target.name());
        }
    }

    private BigDecimal calculateCouponDiscount(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Coupon not found: " + code));

        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon is not active: " + code);
        }

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon has expired: " + code);
        }

        if (coupon.getMinimumOrderAmount() != null && subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BadRequestException("Subtotal must be at least " + coupon.getMinimumOrderAmount() + " to use coupon " + code);
        }

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .min(subtotal);
        } else {
            return coupon.getDiscountValue().min(subtotal);
        }
    }
}
