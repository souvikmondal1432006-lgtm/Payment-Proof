package com.paymentproof.service;

import com.paymentproof.client.MlServiceClient;
import com.paymentproof.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDiagnosticsService {

    private final PaymentRepository paymentRepository;
    private final MlServiceClient mlServiceClient;

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());

        // 1. Backend Self Check
        Map<String, Object> backendHealth = new HashMap<>();
        backendHealth.put("name", "JAVA BACKEND");
        backendHealth.put("description", "Payment Investigation Engine");
        backendHealth.put("status", "HEALTHY");
        backendHealth.put("label", "Ready");

        // 2. Database Check
        Map<String, Object> dbHealth = new HashMap<>();
        dbHealth.put("name", "MYSQL DATABASE");
        dbHealth.put("description", "Payment & Audit Records Store");
        try {
            paymentRepository.count();
            dbHealth.put("status", "HEALTHY");
            dbHealth.put("label", "Ready");
        } catch (Exception e) {
            log.warn("Database health check ping failed: {}", e.getMessage());
            dbHealth.put("status", "OFFLINE");
            dbHealth.put("label", "Unavailable");
        }

        // 3. Python ML Service Check
        Map<String, Object> mlHealth = new HashMap<>();
        mlHealth.put("name", "ML SERVICE");
        mlHealth.put("description", "AI Prediction & Telemetry Classifier");
        boolean mlHealthy = mlServiceClient.isHealthy();
        if (mlHealthy) {
            mlHealth.put("status", "HEALTHY");
            mlHealth.put("label", "Ready");
        } else {
            mlHealth.put("status", "OFFLINE");
            mlHealth.put("label", "Unavailable");
        }

        Map<String, Object> services = new HashMap<>();
        services.put("backend", backendHealth);
        services.put("database", dbHealth);
        services.put("mlService", mlHealth);

        boolean allHealthy = "HEALTHY".equals(dbHealth.get("status")) && "HEALTHY".equals(mlHealth.get("status"));
        boolean anyHealthy = "HEALTHY".equals(dbHealth.get("status")) || "HEALTHY".equals(mlHealth.get("status"));

        health.put("status", allHealthy ? "HEALTHY" : (anyHealthy ? "DEGRADED" : "OFFLINE"));
        health.put("services", services);

        return health;
    }
}
