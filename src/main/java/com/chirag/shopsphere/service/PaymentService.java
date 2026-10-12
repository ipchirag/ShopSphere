package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.PaymentCreateRequest;
import com.chirag.shopsphere.dto.PaymentResponse;
import com.chirag.shopsphere.dto.PaymentStatusUpdateRequest;
import com.chirag.shopsphere.entity.Order;
import com.chirag.shopsphere.entity.OrderStatus;
import com.chirag.shopsphere.entity.Payment;
import com.chirag.shopsphere.entity.PaymentStatus;
import com.chirag.shopsphere.exception.BadRequestException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.OrderRepository;
import com.chirag.shopsphere.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot initiate payment for a cancelled order");
        }

        String paymentRef = "PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .order(order)
                .paymentReference(paymentRef)
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .build();

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Development-only endpoint to simulate payment status transitions.
     * Limitations: Only for local dev/testing without a real payment provider.
     */
    @Transactional
    public PaymentResponse updateSimulatedPaymentStatus(Long id, PaymentStatusUpdateRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        payment.setStatus(request.getStatus());

        if (request.getStatus() == PaymentStatus.SUCCESS) {
            Order order = payment.getOrder();
            if (order != null && order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }
        }

        Payment updated = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(updated);
    }
}
