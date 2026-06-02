package tn.high_concurrency_order.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private String productId;
    private String orderId;
    private Integer quantity;
}