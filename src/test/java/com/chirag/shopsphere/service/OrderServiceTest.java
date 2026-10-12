package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.CreateOrderRequest;
import com.chirag.shopsphere.dto.OrderResponse;
import com.chirag.shopsphere.dto.UpdateOrderStatusRequest;
import com.chirag.shopsphere.entity.Cart;
import com.chirag.shopsphere.entity.CartItem;
import com.chirag.shopsphere.entity.Order;
import com.chirag.shopsphere.entity.OrderItem;
import com.chirag.shopsphere.entity.OrderStatus;
import com.chirag.shopsphere.entity.Product;
import com.chirag.shopsphere.entity.TransactionType;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.entity.UserRole;
import com.chirag.shopsphere.exception.InsufficientStockException;
import com.chirag.shopsphere.exception.InvalidStatusTransitionException;
import com.chirag.shopsphere.repository.CartItemRepository;
import com.chirag.shopsphere.repository.CartRepository;
import com.chirag.shopsphere.repository.CouponRepository;
import com.chirag.shopsphere.repository.InventoryTransactionRepository;
import com.chirag.shopsphere.repository.OrderItemRepository;
import com.chirag.shopsphere.repository.OrderRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private OrderService orderService;

    private User sampleUser;
    private Product sampleProduct;
    private Cart sampleCart;
    private CartItem sampleCartItem;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        sampleProduct = Product.builder()
                .id(10L)
                .name("Wireless Mouse")
                .sku("MOUSE-01")
                .price(new BigDecimal("29.99"))
                .stockQuantity(15)
                .active(true)
                .build();

        sampleCart = Cart.builder()
                .id(100L)
                .user(sampleUser)
                .build();

        sampleCartItem = CartItem.builder()
                .id(500L)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
    }

    @Test
    @DisplayName("Successfully create order from cart, reduce stock, and record inventory transaction")
    void shouldCreateOrderSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(sampleCartItem));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1000L);
            return order;
        });

        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .build();

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("59.98"), response.getSubtotal());
        assertEquals(new BigDecimal("59.98"), response.getTotalAmount());
        assertEquals(13, sampleProduct.getStockQuantity()); // 15 - 2 = 13

        verify(productRepository).save(sampleProduct);
        verify(inventoryTransactionRepository).save(any());
        verify(cartItemRepository).deleteByCartId(100L);
    }

    @Test
    @DisplayName("Throw InsufficientStockException when requested quantity exceeds product stock")
    void shouldFailWhenStockIsInsufficient() {
        sampleProduct.setStockQuantity(1); // Cart needs 2

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(sampleCartItem));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleProduct));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .build();

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));
    }

    @Test
    @DisplayName("Cancel order, restore product stock, and record cancellation transaction")
    void shouldCancelOrderSuccessfully() {
        Order order = Order.builder()
                .id(1000L)
                .orderNumber("ORD-12345678")
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("59.98"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("59.98"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(200L)
                .order(order)
                .product(sampleProduct)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .lineTotal(new BigDecimal("59.98"))
                .build();

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of(item));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleProduct));

        OrderResponse response = orderService.cancelOrder(1000L);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(17, sampleProduct.getStockQuantity()); // 15 + 2 = 17
        verify(inventoryTransactionRepository).save(any());
    }

    @Test
    @DisplayName("Throw InvalidStatusTransitionException for illegal status changes")
    void shouldRejectIllegalStatusTransition() {
        Order order = Order.builder()
                .id(1000L)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest invalidRequest = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED) // Cannot jump from PENDING to DELIVERED
                .build();

        assertThrows(InvalidStatusTransitionException.class, () ->
                orderService.updateOrderStatus(1000L, invalidRequest));
    }
}
