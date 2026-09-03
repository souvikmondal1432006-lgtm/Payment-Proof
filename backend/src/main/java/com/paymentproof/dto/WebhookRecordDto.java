package com.paymentproof.dto;

import com.paymentproof.entity.enums.WebhookDeliveryStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRecordDto {
    private String webhookId;
    private String paymentId;
    private String merchantId;
    private String eventName;
    private String targetUrl;
    private Integer attemptCount;
    private Integer maxAttempts;
    private WebhookDeliveryStatus deliveryStatus;
    private Integer httpStatusCode;
    private Integer latencyMs;
    private String requestPayloadHash;
    private String requestPayload;
    private String responseBody;
    private LocalDateTime firstAttemptAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
}
