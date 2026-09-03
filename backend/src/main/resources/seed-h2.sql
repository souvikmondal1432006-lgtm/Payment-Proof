-- =============================================================================
-- Standalone H2 Seed Data: Complex Multi-Party Contradiction Scenarios
-- =============================================================================

-- Scenario 1: GHOST CAPTURE (Bank=SUCCESS, Gateway=PENDING, Merchant=CANCELLED, Webhook=MISSING)
INSERT INTO transactions (id, reference_id, merchant_id, customer_id, amount, currency, canonical_status, payment_method, created_at, updated_at)
VALUES ('tx_99810a', 'PAY_REF_99810A', 'merch_flipkart_01', 'cust_rahul_sharma', 4499.0000, 'INR', 'CONTRADICTION_DETECTED', 'UPI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO provider_telemetry (id, transaction_id, provider_type, provider_name, reported_status, reported_amount, raw_response_code, raw_response_message, event_timestamp, latency_ms, payload_hash, created_at)
VALUES
('tel_101', 'tx_99810a', 'BANK', 'HDFC_CORE_BANKING', 'SUCCESS', 4499.0000, '00', 'DEBIT_PROCESSED_AC_XXXX1290', CURRENT_TIMESTAMP, 320, 'sha256:4f8a9b1c7d2e3f4a', CURRENT_TIMESTAMP),
('tel_102', 'tx_99810a', 'GATEWAY', 'RAZORPAY_ENGINE', 'PENDING', 4499.0000, 'ASYNC_AWAIT', 'AWAITING_PSP_CALLBACK', CURRENT_TIMESTAMP, 890, 'sha256:2b1c3d4e5f6a7b8c', CURRENT_TIMESTAMP),
('tel_103', 'tx_99810a', 'MERCHANT_APP', 'FLIPKART_CHECKOUT', 'CANCELLED', 4499.0000, 'CART_EXPIRED', 'SESSION_TIMEOUT_NO_PROOF', CURRENT_TIMESTAMP, 120, 'sha256:9c8d7e6f5a4b3c2d', CURRENT_TIMESTAMP),
('tel_104', 'tx_99810a', 'WEBHOOK_SERVICE', 'EVENT_BROKER', 'MISSING', NULL, 'ERR_DISPATCH_TIMEOUT', 'NO_WEBHOOK_DELIVERED_TO_MERCHANT', CURRENT_TIMESTAMP, 5000, 'sha256:0a1b2c3d4e5f6a7b', CURRENT_TIMESTAMP);

INSERT INTO contradiction_incidents (id, transaction_id, contradiction_type, severity, divergence_summary, detected_at, status)
VALUES ('inc_001', 'tx_99810a', 'GHOST_CAPTURE', 'CRITICAL', 'Bank debited INR 4,499.00 (SUCCESS) but Gateway timed out (PENDING), Webhook was never delivered (MISSING), and Merchant marked cart as CANCELLED.', CURRENT_TIMESTAMP, 'OPEN');

INSERT INTO investigation_cases (id, transaction_id, contradiction_id, assigned_operator, case_status, ml_incident_classification, ml_confidence_score, ml_anomaly_score, ml_explanation, ml_recommended_action, ml_analyzed_at, created_at, updated_at)
VALUES (
    'case_inv_001',
    'tx_99810a',
    'inc_001',
    'operator_priya_m',
    'AI_ANALYZED',
    'GHOST_CAPTURE_WEBHOOK_DROPPED',
    0.9842,
    0.9120,
    '{"root_cause": "Bank confirmed fund debit, but Gateway timed out before callback receipt and Webhook never delivered. Merchant abandoned cart.", "bank_verified": true, "gateway_sync_lag_sec": 118, "customer_funds_at_risk": 4499.00, "telemetry_fingerprint": "BANK_SUCCESS|GW_PENDING|MERCH_CANCELLED|WH_MISSING"}',
    'INITIATE_AUTO_REFUND_CUSTOMER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO audit_events (id, entity_type, entity_id, action, actor_type, actor_id, event_payload, ip_address, created_at)
VALUES
('aud_001', 'TRANSACTION', 'tx_99810a', 'TRANSACTION_INITIALIZED', 'SYSTEM', 'PAYMENT_INGESTION_PIPELINE', '{"amount": 4499.0, "currency": "INR"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_002', 'CONTRADICTION', 'inc_001', 'CONTRADICTION_DETECTED', 'SYSTEM', 'STATE_RECONCILIATION_ENGINE', '{"type": "GHOST_CAPTURE", "severity": "CRITICAL"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_003', 'INVESTIGATION_CASE', 'case_inv_001', 'ML_INFERENCE_COMPLETED', 'JAVA_WORKFLOW_ENGINE', 'SPRING_BOOT_CORE', '{"classification": "GHOST_CAPTURE_WEBHOOK_DROPPED", "confidence": 0.9842}', '127.0.0.1', CURRENT_TIMESTAMP);


-- Scenario 2: LATE SETTLEMENT MISMATCH (Bank=SUCCESS, Gateway=FAILED, Merchant=SUCCESS)
INSERT INTO transactions (id, reference_id, merchant_id, customer_id, amount, currency, canonical_status, payment_method, created_at, updated_at)
VALUES ('tx_99810b', 'PAY_REF_99810B', 'merch_swiggy_02', 'cust_ananya_roy', 850.0000, 'INR', 'CONTRADICTION_DETECTED', 'CREDIT_CARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO provider_telemetry (id, transaction_id, provider_type, provider_name, reported_status, reported_amount, raw_response_code, raw_response_message, event_timestamp, latency_ms, payload_hash, created_at)
VALUES
('tel_201', 'tx_99810b', 'BANK', 'ICICI_PAYMENT_GATEWAY', 'SUCCESS', 850.0000, 'TXN_SETTLED_200', 'SETTLEMENT_BATCH_PROCESSED', CURRENT_TIMESTAMP, 135000, 'sha256:7e8f9a0b1c2d3e4f', CURRENT_TIMESTAMP),
('tel_202', 'tx_99810b', 'GATEWAY', 'RAZORPAY_ENGINE', 'FAILED', 850.0000, 'CLIENT_TIMEOUT_ERR', 'SOCKET_CLOSED_BEFORE_3DS', CURRENT_TIMESTAMP, 45000, 'sha256:6d5c4b3a2f1e0d9c', CURRENT_TIMESTAMP),
('tel_203', 'tx_99810b', 'MERCHANT_APP', 'SWIGGY_ORDER_OMS', 'SUCCESS', 850.0000, 'ORDER_DISPATCHED', 'FOOD_PREPARED_AND_ASSIGNED_RIDER', CURRENT_TIMESTAMP, 80, 'sha256:3a2b1c0d9e8f7a6b', CURRENT_TIMESTAMP),
('tel_204', 'tx_99810b', 'WEBHOOK_SERVICE', 'EVENT_BROKER', 'DELIVERED_FAILED_STATUS', 850.0000, 'HTTP_200', 'MERCHANT_RECEIVED_FAILED_EVENT', CURRENT_TIMESTAMP, 150, 'sha256:5f4e3d2c1b0a9f8e', CURRENT_TIMESTAMP);

INSERT INTO contradiction_incidents (id, transaction_id, contradiction_type, severity, divergence_summary, detected_at, status)
VALUES ('inc_002', 'tx_99810b', 'LATE_SETTLEMENT_MISMATCH', 'HIGH', 'Gateway marked transaction as FAILED on client timeout, but Bank asynchronously settled funds (SUCCESS) while Merchant fulfilled the order.', CURRENT_TIMESTAMP, 'OPEN');

INSERT INTO investigation_cases (id, transaction_id, contradiction_id, assigned_operator, case_status, ml_incident_classification, ml_confidence_score, ml_anomaly_score, ml_explanation, ml_recommended_action, ml_analyzed_at, created_at, updated_at)
VALUES (
    'case_inv_002',
    'tx_99810b',
    'inc_002',
    'operator_arjun_k',
    'AI_ANALYZED',
    'LATE_SETTLEMENT_ASYNC_CAPTURE',
    0.9510,
    0.7850,
    '{"root_cause": "Gateway aborted on client socket close, but Acquirer bank settled transaction asynchronously. Merchant fulfilled order.", "risk_of_double_charge": false, "merchant_loss_exposure": 0.00}',
    'FORCE_SETTLE_MERCHANT',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO audit_events (id, entity_type, entity_id, action, actor_type, actor_id, event_payload, ip_address, created_at)
VALUES
('aud_004', 'TRANSACTION', 'tx_99810b', 'TRANSACTION_INITIALIZED', 'SYSTEM', 'PAYMENT_INGESTION_PIPELINE', '{"amount": 850.0, "currency": "INR"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_005', 'CONTRADICTION', 'inc_002', 'CONTRADICTION_DETECTED', 'SYSTEM', 'STATE_RECONCILIATION_ENGINE', '{"type": "LATE_SETTLEMENT_MISMATCH", "severity": "HIGH"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_006', 'INVESTIGATION_CASE', 'case_inv_002', 'ML_INFERENCE_COMPLETED', 'JAVA_WORKFLOW_ENGINE', 'SPRING_BOOT_CORE', '{"classification": "LATE_SETTLEMENT_ASYNC_CAPTURE", "confidence": 0.9510}', '127.0.0.1', CURRENT_TIMESTAMP);


-- Scenario 3: SILENT DEBIT REVERSAL (Bank=DEBIT_REVERSED, Gateway=SUCCESS, Merchant=FULFILLED)
INSERT INTO transactions (id, reference_id, merchant_id, customer_id, amount, currency, canonical_status, payment_method, created_at, updated_at)
VALUES ('tx_99810c', 'PAY_REF_99810C', 'merch_zomato_03', 'cust_vikram_singh', 1250.0000, 'INR', 'CONTRADICTION_DETECTED', 'NET_BANKING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO provider_telemetry (id, transaction_id, provider_type, provider_name, reported_status, reported_amount, raw_response_code, raw_response_message, event_timestamp, latency_ms, payload_hash, created_at)
VALUES
('tel_301', 'tx_99810c', 'BANK', 'AXIS_NETBANKING', 'DEBIT_REVERSED', 1250.0000, 'REV_01', 'AUTO_REVERSAL_INTERNAL_BANK_GL', CURRENT_TIMESTAMP, 190000, 'sha256:1a2b3c4d5e6f7a8b', CURRENT_TIMESTAMP),
('tel_302', 'tx_99810c', 'GATEWAY', 'RAZORPAY_ENGINE', 'SUCCESS', 1250.0000, 'CAPTURED', 'PAYMENT_CAPTURE_ACK', CURRENT_TIMESTAMP, 450, 'sha256:4b3c2a1f0e9d8c7b', CURRENT_TIMESTAMP),
('tel_303', 'tx_99810c', 'MERCHANT_APP', 'ZOMATO_OMS', 'FULFILLED', 1250.0000, 'DELIVERED', 'ORDER_DELIVERED_TO_CUSTOMER', CURRENT_TIMESTAMP, 210, 'sha256:8f7e6d5c4b3a2f1e', CURRENT_TIMESTAMP);

INSERT INTO contradiction_incidents (id, transaction_id, contradiction_type, severity, divergence_summary, detected_at, status)
VALUES ('inc_003', 'tx_99810c', 'SILENT_DEBIT_REVERSAL', 'HIGH', 'Bank reversed the customer debit silently after authorization. Gateway marked SUCCESS and Merchant fulfilled order.', CURRENT_TIMESTAMP, 'OPEN');

INSERT INTO investigation_cases (id, transaction_id, contradiction_id, assigned_operator, case_status, ml_incident_classification, ml_confidence_score, ml_anomaly_score, ml_explanation, ml_recommended_action, ml_analyzed_at, created_at, updated_at)
VALUES (
    'case_inv_003',
    'tx_99810c',
    'inc_003',
    NULL,
    'OPEN',
    'UNANNOUNCED_ISSUER_AUTO_REVERSAL',
    0.9130,
    0.8640,
    '{"root_cause": "Issuer core banking engine reversed debit during internal ledger reconcile without dispatching reversal webhook.", "financial_loss_liability": "MERCHANT_UNPROTECTED"}',
    'MANUAL_BANK_ESCALATION',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO audit_events (id, entity_type, entity_id, action, actor_type, actor_id, event_payload, ip_address, created_at)
VALUES
('aud_007', 'TRANSACTION', 'tx_99810c', 'TRANSACTION_INITIALIZED', 'SYSTEM', 'PAYMENT_INGESTION_PIPELINE', '{"amount": 1250.0, "currency": "INR"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_008', 'CONTRADICTION', 'inc_003', 'CONTRADICTION_DETECTED', 'SYSTEM', 'STATE_RECONCILIATION_ENGINE', '{"type": "SILENT_DEBIT_REVERSAL", "severity": "HIGH"}', '127.0.0.1', CURRENT_TIMESTAMP);


-- Scenario 4: WEBHOOK DISPATCH TIMEOUT ON SUCCESSFUL ORDER
INSERT INTO transactions (id, reference_id, merchant_id, customer_id, amount, currency, canonical_status, payment_method, created_at, updated_at)
VALUES ('tx_99810d', 'PAY_REF_99810D', 'merch_bookmyshow_04', 'cust_rohit_verma', 720.0000, 'INR', 'CONTRADICTION_DETECTED', 'UPI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO provider_telemetry (id, transaction_id, provider_type, provider_name, reported_status, reported_amount, raw_response_code, raw_response_message, event_timestamp, latency_ms, payload_hash, created_at)
VALUES
('tel_401', 'tx_99810d', 'BANK', 'SBI_UPI_SWITCH', 'SUCCESS', 720.0000, '00', 'DEBIT_SUCCESS_PSP', CURRENT_TIMESTAMP, 250, 'sha256:5a4b3c2d1e0f9a8b', CURRENT_TIMESTAMP),
('tel_402', 'tx_99810d', 'GATEWAY', 'RAZORPAY_ENGINE', 'SUCCESS', 720.0000, 'CAPTURED', 'AUTH_AND_CAPTURE_SUCCESS', CURRENT_TIMESTAMP, 310, 'sha256:9f8e7d6c5b4a3f2e', CURRENT_TIMESTAMP),
('tel_403', 'tx_99810d', 'MERCHANT_APP', 'BMS_SEAT_LOCK', 'CANCELLED', 720.0000, 'SEAT_LOCK_EXPIRED', 'WEBHOOK_TIMEOUT_SEATS_RELEASED', CURRENT_TIMESTAMP, 110, 'sha256:1b2c3d4e5f6a7b8c', CURRENT_TIMESTAMP),
('tel_404', 'tx_99810d', 'WEBHOOK_SERVICE', 'EVENT_BROKER', 'MISSING', NULL, 'ERR_CONNECT_RESET', 'MERCHANT_INGRESS_TIMEOUT', CURRENT_TIMESTAMP, 5000, 'sha256:7c8b9a0d1e2f3a4b', CURRENT_TIMESTAMP);

INSERT INTO contradiction_incidents (id, transaction_id, contradiction_type, severity, divergence_summary, detected_at, status)
VALUES ('inc_004', 'tx_99810d', 'WEBHOOK_DROP_ORDER_ABANDONED', 'HIGH', 'Payment succeeded at Bank and Gateway, but merchant order timed out due to dropped webhook.', CURRENT_TIMESTAMP, 'OPEN');

INSERT INTO investigation_cases (id, transaction_id, contradiction_id, assigned_operator, case_status, ml_incident_classification, ml_confidence_score, ml_anomaly_score, ml_explanation, ml_recommended_action, ml_analyzed_at, created_at, updated_at)
VALUES (
    'case_inv_004',
    'tx_99810d',
    'inc_004',
    NULL,
    'OPEN',
    'WEBHOOK_DISPATCH_TIMEOUT_ORDER_ABANDONED',
    0.9400,
    0.8200,
    '{"root_cause": "Payment succeeded at Bank and Gateway, but merchant order timed out due to delayed/failed webhook.", "recommendation": "Resend authoritative webhook with idempotency token."}',
    'RESEND_AUTHORITATIVE_WEBHOOK',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO audit_events (id, entity_type, entity_id, action, actor_type, actor_id, event_payload, ip_address, created_at)
VALUES
('aud_009', 'TRANSACTION', 'tx_99810d', 'TRANSACTION_INITIALIZED', 'SYSTEM', 'PAYMENT_INGESTION_PIPELINE', '{"amount": 720.0, "currency": "INR"}', '127.0.0.1', CURRENT_TIMESTAMP),
('aud_010', 'CONTRADICTION', 'inc_004', 'CONTRADICTION_DETECTED', 'SYSTEM', 'STATE_RECONCILIATION_ENGINE', '{"type": "WEBHOOK_DROP_ORDER_ABANDONED", "severity": "HIGH"}', '127.0.0.1', CURRENT_TIMESTAMP);
