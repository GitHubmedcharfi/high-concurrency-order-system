package tn.high_concurrency_order.system.service;

import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.high_concurrency_order.system.dto.ReservationRequest;
import tn.high_concurrency_order.system.service.InventoryService;
import tn.high_concurrency_order.system.dto.OrderRequest;
import tn.high_concurrency_order.system.dto.OrderResponse;
import tn.high_concurrency_order.system.entity.Order;
import tn.high_concurrency_order.system.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        String orderId = UUID.randomUUID().toString();

        // Réserver le stock automatiquement
        ReservationRequest reservationRequest = ReservationRequest.builder()
                .productId(request.getProductId())
                .orderId(orderId)
                .quantity(request.getQuantity())
                .build();

        inventoryService.reserveStock(reservationRequest);

        // Créer l'order
        Order order = Order.builder()
                .orderId(orderId)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(request.getTotalPrice())
                .status(Order.OrderStatus.PENDING)
                .build();

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order introuvable : " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order ne peut pas être confirmé — statut actuel : " + order.getStatus());
        }

        // Confirmer la réservation
        inventoryService.confirmReservation(orderId);

        order.setStatus(Order.OrderStatus.CONFIRMED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order introuvable : " + orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order déjà annulé");
        }

        // Annuler la réservation et restaurer le stock
        inventoryService.cancelReservation(orderId);

        order.setStatus(Order.OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse getOrder(String orderId) {
        return toResponse(orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order introuvable : " + orderId)));
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderId(o.getOrderId())
                .productId(o.getProductId())
                .quantity(o.getQuantity())
                .totalPrice(o.getTotalPrice())
                .status(o.getStatus().name())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
}