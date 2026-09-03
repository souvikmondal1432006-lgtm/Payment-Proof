-- Standalone H2 Schema for zero-dependency local development
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(64) PRIMARY KEY,
    reference_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    amount DECIMAL(15, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    canonical_status VARCHAR(64) NOT NULL DEFAULT 'INITIALIZED',
    payment_method VARCHAR(32) NOT NULL DEFAULT 'UPI',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS provider_telemetry (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    provider_type VARCHAR(64) NOT NULL,
    provider_name VARCHAR(64) NOT NULL,
    reported_status VARCHAR(64) NOT NULL,
    reported_amount DECIMAL(15, 4) NULL,
    raw_response_code VARCHAR(64) NULL,
    raw_response_message TEXT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    latency_ms INT NULL,
    payload_hash VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contradiction_incidents (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    contradiction_type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL DEFAULT 'HIGH',
    divergence_summary TEXT NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(64) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS investigation_cases (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    contradiction_id VARCHAR(64) NOT NULL,
    assigned_operator VARCHAR(64) NULL,
    case_status VARCHAR(64) NOT NULL DEFAULT 'OPEN',
    ml_incident_classification VARCHAR(64) NULL,
    ml_confidence_score DECIMAL(5, 4) NULL,
    ml_anomaly_score DECIMAL(5, 4) NULL,
    ml_explanation TEXT NULL,
    ml_recommended_action VARCHAR(64) NULL,
    ml_analyzed_at TIMESTAMP NULL,
    authoritative_action VARCHAR(64) NULL,
    resolution_notes TEXT NULL,
    resolved_by VARCHAR(64) NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_type VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    event_payload TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
