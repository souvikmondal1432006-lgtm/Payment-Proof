package com.paymentproof.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEventDto {
    private String eventId;
    private String paymentId;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String eventSource;
    private String eventPayload;
    private LocalDateTime eventTimestamp;
}
