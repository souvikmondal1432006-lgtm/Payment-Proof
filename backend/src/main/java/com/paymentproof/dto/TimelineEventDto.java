package com.paymentproof.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEventDto {
    private String eventId;
    private String source; // BANK, GATEWAY, MERCHANT, WEBHOOK, PAYMENT_EVENT, AUDIT
    private String eventType;
    private String title;
    private String description;
    private String status;
    private Integer latencyMs;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
