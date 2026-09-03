package com.paymentproof.client.ml;

import com.paymentproof.client.ml.dto.MlClassificationRequest;
import com.paymentproof.client.ml.dto.MlClassificationResponse;

import java.util.Map;

public interface MlServiceClient {
    
    /**
     * Calls Python FastAPI ML Service to classify payment contradictions.
     * This call is strictly advisory and produces inference metadata without DB mutations.
     */
    MlClassificationResponse classifyIncident(MlClassificationRequest request);

    /**
     * Checks if downstream Python ML service is healthy.
     */
    Map<String, Object> checkHealth();
}
