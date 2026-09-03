package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributingSignalDto {

    @JsonProperty("signal_name")
    private String signalName;

    @JsonProperty("signal_value")
    private String signalValue;

    @JsonProperty("importance_weight")
    private Double importanceWeight;

    @JsonProperty("interpretation")
    private String interpretation;
}
