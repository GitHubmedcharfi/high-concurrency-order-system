package tn.high_concurrency_order.system.dto;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}