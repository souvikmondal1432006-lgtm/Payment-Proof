package com.paymentproof.dto;

import com.paymentproof.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private String paymentId;
    private String merchantId;
    private String customerId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentMethodSubtype;
    private PaymentStatus status;
    private String clientIp;
    private String userAgent;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}
