package com.paymentproof.client.ml.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlTelemetryPayload {
    private String provider_type;
    private String provider_name;
    private String reported_status;
    private BigDecimal reported_amount;
    private String raw_response_code;
    private String raw_response_message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime event_timestamp;

    private Integer latency_ms;
    private String payload_hash;
}
