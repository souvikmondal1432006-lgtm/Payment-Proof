package com.paymentproof.client.ml.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlFeatureImportance {
    private String feature_name;
    private Double weight;
    private String description;
    private Object evidence_value;
}
