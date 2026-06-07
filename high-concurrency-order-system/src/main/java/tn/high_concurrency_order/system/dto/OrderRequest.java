package tn.high_concurrency_order.system.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    private String productId;
    private Integer quantity;
    private BigDecimal totalPrice;
}