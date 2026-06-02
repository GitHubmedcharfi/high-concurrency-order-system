package tn.high_concurrency_order.system.service;


import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tn.high_concurrency_order.system.dto.InventoryRequest;
import tn.high_concurrency_order.system.dto.InventoryResponse;
import tn.high_concurrency_order.system.dto.ReservationRequest;
import tn.high_concurrency_order.system.dto.ReservationResponse;
import tn.high_concurrency_order.system.entity.Inventory;
import tn.high_concurrency_order.system.entity.StockReservation;
import tn.high_concurrency_order.system.repository.InventoryRepository;
import tn.high_concurrency_order.system.repository.StockReservationRepository;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        Inventory inventory = Inventory.builder()
            .productId(request.getProductId())
            .productName(request.getProductName())
            .totalQuantity(request.getQuantity())
            .availableQuantity(request.getQuantity())
            .build();
        return toResponse(inventoryRepository.save(inventory));
    }

    // Optimistic Lock — 90% des cas
    @Transactional
    public ReservationResponse reserveStock(ReservationRequest request) {
        try {
            Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit introuvable : " + request.getProductId()));

            if (inventory.getAvailableQuantity() < request.getQuantity()) {
                throw new RuntimeException("Stock insuffisant");
            }

            inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - request.getQuantity()
            );
            inventoryRepository.save(inventory);

            StockReservation reservation = StockReservation.builder()
                .inventory(inventory)
                .orderId(request.getOrderId())
                .quantity(request.getQuantity())
                .status(StockReservation.ReservationStatus.PENDING)
                .build();

            return toReservationResponse(reservationRepository.save(reservation));

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Conflit de stock — réessayez");
        }
    }

    // Pessimistic Lock — 10% cas critiques
    @Transactional
    public ReservationResponse reserveStockCritical(ReservationRequest request) {
        Inventory inventory = inventoryRepository
            .findByProductIdWithLock(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new RuntimeException("Stock insuffisant");
        }

        inventory.setAvailableQuantity(
            inventory.getAvailableQuantity() - request.getQuantity()
        );
        inventoryRepository.save(inventory);

        StockReservation reservation = StockReservation.builder()
            .inventory(inventory)
            .orderId(request.getOrderId())
            .quantity(request.getQuantity())
            .status(StockReservation.ReservationStatus.PENDING)
            .build();

        return toReservationResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void cancelReservation(String orderId) {
        StockReservation reservation = reservationRepository
            .findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        Inventory inventory = reservation.getInventory();
        inventory.setAvailableQuantity(
            inventory.getAvailableQuantity() + reservation.getQuantity()
        );

        reservation.setStatus(StockReservation.ReservationStatus.CANCELLED);
        inventoryRepository.save(inventory);
        reservationRepository.save(reservation);
    }

    public InventoryResponse getInventory(String productId) {
        return toResponse(inventoryRepository
            .findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Produit introuvable")));
    }

    private InventoryResponse toResponse(Inventory i) {
        return InventoryResponse.builder()
            .id(i.getId())
            .productId(i.getProductId())
            .productName(i.getProductName())
            .totalQuantity(i.getTotalQuantity())
            .availableQuantity(i.getAvailableQuantity())
            .build();
    }

    private ReservationResponse toReservationResponse(StockReservation r) {
        return ReservationResponse.builder()
            .id(r.getId())
            .orderId(r.getOrderId())
            .quantity(r.getQuantity())
            .status(r.getStatus().name())
            .reservedAt(r.getReservedAt())
            .expiresAt(r.getExpiresAt())
            .build();
    }
}