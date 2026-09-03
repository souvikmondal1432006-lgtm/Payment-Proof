package com.paymentproof.dto;

import com.paymentproof.entity.enums.FulfillmentStatus;
import com.paymentproof.entity.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOrderRecordDto {
    private String merchantOrderRecordId;
    private String paymentId;
    private String merchantId;
    private String merchantOrderId;
    private OrderStatus orderStatus;
    private FulfillmentStatus fulfillmentStatus;
    private BigDecimal expectedAmount;
    private String currency;
    private String cancellationReason;
    private String customerNotes;
    private LocalDateTime merchantUpdatedAt;
    private LocalDateTime createdAt;
}
