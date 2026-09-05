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
    private String sourceSystem;
    private String eventType;
    private String title;
    private String description;
    private String status;
    private String previousState;
    private String newState;
    private Integer latencyMs;
    private LocalDateTime timestamp;
    private LocalDateTime eventTimestamp;
    private Map<String, Object> metadata;
}
