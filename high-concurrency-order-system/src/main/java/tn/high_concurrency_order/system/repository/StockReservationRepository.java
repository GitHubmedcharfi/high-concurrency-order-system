package tn.high_concurrency_order.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.high_concurrency_order.system.entity.StockReservation;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByInventoryIdAndStatus(
        Long inventoryId,
        StockReservation.ReservationStatus status
    );
}