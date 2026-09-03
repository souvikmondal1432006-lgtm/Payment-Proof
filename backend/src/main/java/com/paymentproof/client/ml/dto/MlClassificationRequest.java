package com.paymentproof.client.ml.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlClassificationRequest {
    private String transaction_id;
    private String reference_id;
    private BigDecimal amount;
    private String currency;
    private String payment_method;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime created_at;

    private List<MlTelemetryPayload> telemetries;
}
