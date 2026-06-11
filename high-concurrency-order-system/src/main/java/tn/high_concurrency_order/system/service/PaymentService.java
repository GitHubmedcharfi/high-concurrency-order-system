package tn.high_concurrency_order.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.high_concurrency_order.system.dto.PaymentRequest;
import tn.high_concurrency_order.system.dto.PaymentResponse;
import tn.high_concurrency_order.system.entity.Order;
import tn.high_concurrency_order.system.entity.Payment;
import tn.high_concurrency_order.system.repository.OrderRepository;
import tn.high_concurrency_order.system.repository.PaymentRepository;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        // Vérifier que l'order existe et est CONFIRMED
        Order order = orderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order introuvable : " + request.getOrderId()));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order doit être CONFIRMED pour être payé — statut actuel : " + order.getStatus());
        }

        // Vérifier qu'il n'y a pas déjà un paiement COMPLETED
        paymentRepository.findByOrderId(request.getOrderId())
                .ifPresent(p -> {
                    if (p.getStatus() == Payment.PaymentStatus.COMPLETED) {
                        throw new RuntimeException("Order déjà payé");
                    }
                });

        String paymentId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // Simuler le traitement du paiement
        try {
            simulatePaymentProcessing(request.getAmount());
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Annuler l'order et restaurer le stock si paiement échoue
            orderService.cancelOrder(request.getOrderId());

            throw new RuntimeException("Paiement échoué : " + e.getMessage());
        }

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse refundPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable : " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Seul un paiement COMPLETED peut être remboursé");
        }

        // Annuler l'order et restaurer le stock
        orderService.cancelOrder(payment.getOrderId());

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse getPayment(String paymentId) {
        return toResponse(paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable : " + paymentId)));
    }

    public PaymentResponse getPaymentByOrder(String orderId) {
        return toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable pour l'order : " + orderId)));
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Simule le traitement — remplace par un vrai provider (Stripe, etc.) en prod
    private void simulatePaymentProcessing(java.math.BigDecimal amount) {
        if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Montant invalide");
        }
        // Simule un échec pour les montants > 50000
        if (amount.compareTo(new java.math.BigDecimal("50000")) > 0) {
            throw new RuntimeException("Montant dépasse la limite autorisée");
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentId(p.getPaymentId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}