package com.paymentproof.client.ml.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlClassificationResponse {
    private String transaction_id;
    private String predicted_classification;
    private BigDecimal confidence_score;
    private BigDecimal anomaly_score;
    private String root_cause_hypothesis;
    private String recommended_action_hypothesis;
    private List<MlFeatureImportance> feature_importances;
    private Map<String, Object> explanation_metadata;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS][XXX]")
    private LocalDateTime analyzed_at;
}
