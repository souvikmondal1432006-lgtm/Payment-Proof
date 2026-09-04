-- =============================================================================
-- PAYMENT PROOF: Cloud Production Relational Schema (MySQL 8.0+)
-- Multi-Party Payment Discrepancy & Forensic Investigation Platform
-- 
-- Cloud-Safe Invariants:
-- 1. Idempotent: Uses CREATE TABLE IF NOT EXISTS for zero data-loss on re-execution.
-- 2. Non-Destructive: NO DROP DATABASE, DROP TABLE, or TRUNCATE statements.
-- 3. Environment-Agnostic: NO hardcoded CREATE DATABASE or USE statements.
-- 4. Complete Alignment: 100% synchronized with JPA entity classes and cryptographic audit ledger.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. PAYMENTS
-- Canonical payment attempt record representing customer intent to pay.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_method ENUM('UPI', 'CREDIT_CARD', 'DEBIT_CARD', 'NET_BANKING', 'WALLET', 'EMI') NOT NULL,
    payment_method_subtype VARCHAR(32) NULL COMMENT 'e.g. GPAY, PHONEPE, HDFC_NETBANKING, VISA, MASTERCARD',
    status ENUM(
        'INITIATED',
        'PENDING',
        'SUCCESS',
        'FAILED',
        'DISPUTED',
        'REFUNDED',
        'PARTIALLY_REFUNDED',
        'CANCELLED',
        'FLAGGED'
    ) NOT NULL DEFAULT 'INITIATED',
    client_ip VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    initiated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at TIMESTAMP(3) NULL,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    INDEX idx_payments_merchant (merchant_id),
    INDEX idx_payments_customer (customer_id),
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_method (payment_method),
    INDEX idx_payments_initiated (initiated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. PAYMENT_EVENTS
-- High-resolution lifecycle event log tracking state transitions and timeline.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_events (
    event_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL COMMENT 'e.g. PAYMENT_INITIATED, AUTHENTICATION_REQUESTED, BANK_DEBIT_ACK, PAYMENT_CAPTURED',
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    event_source ENUM(
        'CLIENT_SDK',
        'GATEWAY_ENGINE',
        'BANK_CONNECTOR',
        'MERCHANT_BACKEND',
        'SETTLEMENT_CRON',
        'RECONCILIATION_SERVICE',
        'OPERATOR_PORTAL'
    ) NOT NULL,
    event_payload JSON NULL,
    event_timestamp TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_pevt_payment (payment_id),
    INDEX idx_pevt_type (event_type),
    INDEX idx_pevt_timestamp (event_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. BANK_RECORDS
-- Core banking system (CBS), UPI switch (NPCI), and Card Network telemetry.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bank_records (
    bank_record_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    bank_name VARCHAR(64) NOT NULL COMMENT 'e.g. HDFC_BANK, ICICI_BANK, SBI, AXIS_BANK, KOTAK_BANK',
    bank_reference_number VARCHAR(64) NULL COMMENT 'Bank internal Reference Number / RRN',
    utr_number VARCHAR(64) NULL COMMENT '12-digit Unique Transaction Reference for UPI/IMPS/NEFT',
    account_last4 VARCHAR(4) NULL,
    bank_status ENUM('PENDING', 'SUCCESS', 'DEBITED', 'FAILED', 'REVERSED', 'DECLINED', 'TIMEOUT') NOT NULL,
    debited_amount DECIMAL(12, 2) NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    response_code VARCHAR(32) NULL COMMENT 'e.g. 00 (Success), U30 (PSP Timeout), ZA (Declined), 91 (Switch Inoperative)',
    response_message TEXT NULL,
    network_latency_ms INT NULL,
    bank_timestamp TIMESTAMP(3) NOT NULL,
    raw_payload JSON NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_bank_payment (payment_id),
    INDEX idx_bank_utr (utr_number),
    INDEX idx_bank_ref (bank_reference_number),
    INDEX idx_bank_status (bank_status),
    INDEX idx_bank_timestamp (bank_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. GATEWAY_RECORDS
-- Payment aggregator and gateway processor telemetry (e.g. Razorpay, PayU, Cashfree).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gateway_records (
    gateway_record_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    gateway_name VARCHAR(64) NOT NULL COMMENT 'e.g. RAZORPAY, CASHFREE, PAYU, BILLDESK, STRIPE',
    gateway_transaction_id VARCHAR(64) NULL,
    gateway_order_id VARCHAR(64) NULL,
    auth_status ENUM('NONE', 'AUTHORIZED', 'FAILED', 'TIMEOUT') NOT NULL DEFAULT 'NONE',
    capture_status ENUM('NOT_REQUESTED', 'PENDING', 'CAPTURED', 'FAILED', 'AUTO_REFUNDED') NOT NULL DEFAULT 'NOT_REQUESTED',
    gateway_status ENUM('PENDING', 'AUTHORIZED', 'SUCCESS', 'FAILED', 'TIMED_OUT', 'CANCELLED') NOT NULL,
    authorized_amount DECIMAL(12, 2) NULL,
    captured_amount DECIMAL(12, 2) NULL,
    fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    error_code VARCHAR(64) NULL COMMENT 'e.g. BAD_REQUEST_ERROR, GATEWAY_ERROR, PAYMENT_TIMED_OUT',
    error_description TEXT NULL,
    processing_latency_ms INT NULL,
    gateway_timestamp TIMESTAMP(3) NOT NULL,
    raw_payload JSON NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_gw_payment (payment_id),
    INDEX idx_gw_txn_id (gateway_transaction_id),
    INDEX idx_gw_status (gateway_status),
    INDEX idx_gw_timestamp (gateway_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. MERCHANT_ORDER_RECORDS
-- Merchant checkout and Order Management System (OMS) cart & fulfillment telemetry.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchant_order_records (
    merchant_order_record_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    merchant_order_id VARCHAR(64) NOT NULL,
    order_status ENUM(
        'DRAFT',
        'PENDING_PAYMENT',
        'PAID',
        'CANCELLED',
        'EXPIRED',
        'FAILED',
        'REFUNDED'
    ) NOT NULL,
    fulfillment_status ENUM(
        'UNFULFILLED',
        'PROCESSING',
        'PACKED',
        'SHIPPED',
        'DELIVERED',
        'CANCELLED',
        'RETURNED'
    ) NOT NULL DEFAULT 'UNFULFILLED',
    expected_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    cancellation_reason VARCHAR(255) NULL,
    customer_notes TEXT NULL,
    merchant_updated_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_mor_payment (payment_id),
    INDEX idx_mor_order (merchant_order_id),
    INDEX idx_mor_merchant (merchant_id),
    INDEX idx_mor_status (order_status),
    INDEX idx_mor_fulfillment (fulfillment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. WEBHOOK_RECORDS
-- Webhook notification dispatch and delivery logs from Gateway to Merchant.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS webhook_records (
    webhook_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    event_name VARCHAR(64) NOT NULL COMMENT 'e.g. payment.captured, payment.failed, refund.processed',
    target_url VARCHAR(512) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 3,
    delivery_status ENUM(
        'SCHEDULED',
        'PENDING',
        'DELIVERED',
        'FAILED',
        'DROPPED',
        'TIMED_OUT',
        'RETRYING'
    ) NOT NULL,
    http_status_code INT NULL COMMENT 'e.g. 200, 404, 500, 504',
    latency_ms INT NULL,
    request_payload_hash VARCHAR(64) NULL,
    request_payload JSON NULL,
    response_body TEXT NULL,
    first_attempt_at TIMESTAMP(3) NOT NULL,
    last_attempt_at TIMESTAMP(3) NULL,
    next_retry_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_wh_payment (payment_id),
    INDEX idx_wh_merchant (merchant_id),
    INDEX idx_wh_status (delivery_status),
    INDEX idx_wh_event (event_name),
    INDEX idx_wh_first_attempt (first_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. SETTLEMENT_RECORDS
-- Payout reconciliation ledger for funds settled from Gateway to Merchant.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS settlement_records (
    settlement_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    batch_id VARCHAR(64) NULL,
    gross_amount DECIMAL(12, 2) NOT NULL,
    fee_deducted DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_deducted DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    net_settled_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    settlement_status ENUM(
        'PENDING',
        'PROCESSING',
        'SETTLED',
        'ON_HOLD',
        'FAILED',
        'REVERSED',
        'DISCREPANCY'
    ) NOT NULL,
    settlement_utr VARCHAR(64) NULL,
    settlement_bank_account VARCHAR(32) NULL,
    settled_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_set_payment (payment_id),
    INDEX idx_set_merchant (merchant_id),
    INDEX idx_set_batch (batch_id),
    INDEX idx_set_status (settlement_status),
    INDEX idx_set_settled_at (settled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. REFUND_RECORDS
-- Refund lifecycle and banking reversal tracking.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refund_records (
    refund_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    gateway_refund_id VARCHAR(64) NULL,
    refund_arn VARCHAR(64) NULL COMMENT 'Acquirer Reference Number for card/bank trace',
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    refund_reason VARCHAR(255) NOT NULL,
    refund_speed ENUM('NORMAL', 'INSTANT') NOT NULL DEFAULT 'NORMAL',
    refund_status ENUM(
        'INITIATED',
        'PENDING',
        'PROCESSED',
        'FAILED',
        'REVERSED',
        'MANUAL_INTERVENTION_REQUIRED'
    ) NOT NULL,
    bank_reversal_status ENUM(
        'NOT_INITIATED',
        'AWAITING_ACK',
        'CREDITED_TO_CUSTOMER',
        'REJECTED_BY_BANK'
    ) NOT NULL DEFAULT 'NOT_INITIATED',
    initiated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at TIMESTAMP(3) NULL,
    raw_response JSON NULL,
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_ref_payment (payment_id),
    INDEX idx_ref_merchant (merchant_id),
    INDEX idx_ref_status (refund_status),
    INDEX idx_ref_arn (refund_arn),
    INDEX idx_ref_initiated (initiated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. INCIDENT_CASES
-- Root discrepancy and contradiction investigation cases.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_cases (
    incident_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    incident_type ENUM(
        'DELAYED_CONFIRMATION',
        'BANK_DEBIT_GATEWAY_FAILURE',
        'GATEWAY_SUCCESS_MISSING_WEBHOOK',
        'DUPLICATE_PAYMENT',
        'REFUND_UNCERTAINTY',
        'SETTLEMENT_MISMATCH',
        'MERCHANT_CANCELLATION_BEFORE_CONFIRMATION',
        'CONFLICTING_PAYMENT_STATES',
        'NORMAL_PAYMENT_FALSE_ALARM',
        'GENUINELY_UNRESOLVED_CASE'
    ) NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    case_status ENUM(
        'OPEN',
        'IN_REVIEW',
        'AI_ANALYZED',
        'NEEDS_REVIEW',
        'RESOLVED',
        'CLOSED_UNRESOLVED',
        'ESCALATED_TO_BANK'
    ) NOT NULL DEFAULT 'OPEN',
    trigger_source ENUM(
        'AUTOMATED_RECONCILIATION',
        'MERCHANT_DISPUTE',
        'CUSTOMER_TICKET',
        'WEBHOOK_MONITOR',
        'SETTLEMENT_AUDITOR',
        'MANUAL_OPERATOR'
    ) NOT NULL,
    assigned_investigator VARCHAR(64) NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    opened_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at TIMESTAMP(3) NULL,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_inc_payment (payment_id),
    INDEX idx_inc_type (incident_type),
    INDEX idx_inc_status (case_status),
    INDEX idx_inc_severity (severity),
    INDEX idx_inc_opened_at (opened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 10. INVESTIGATION_EVIDENCE
-- Multi-source telemetry artifacts, network logs, and digital proof items.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS investigation_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL,
    payment_id VARCHAR(64) NOT NULL,
    evidence_source ENUM(
        'CORE_BANKING_LOG',
        'NPCI_UPI_SWITCH',
        'GATEWAY_TELEMETRY',
        'MERCHANT_OMS_LOG',
        'WEBHOOK_DELIVERY_LOG',
        'SETTLEMENT_LEDGER',
        'CUSTOMER_UPLOAD',
        'OPERATOR_NOTE'
    ) NOT NULL,
    evidence_type ENUM(
        'ISO_8583_PAYLOAD',
        'JSON_TELEMETRY',
        'HTTP_TRACE',
        'ACCOUNT_STATEMENT',
        'SCREENSHOT',
        'CSV_BATCH_ROW',
        'TEXT_NOTE'
    ) NOT NULL,
    file_path VARCHAR(255) NULL,
    raw_content TEXT NULL,
    payload_checksum VARCHAR(64) NULL,
    captured_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (incident_id) REFERENCES incident_cases(incident_id) ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_evi_incident (incident_id),
    INDEX idx_evi_payment (payment_id),
    INDEX idx_evi_source (evidence_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 11. ML_ASSESSMENTS
-- Machine Learning model diagnostics, root cause prediction, and anomaly scores.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ml_assessments (
    assessment_id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL,
    payment_id VARCHAR(64) NOT NULL,
    model_version VARCHAR(64) NOT NULL COMMENT 'e.g. recon-classifier-v2.1',
    predicted_root_cause VARCHAR(128) NOT NULL,
    anomaly_score DECIMAL(5, 4) NOT NULL COMMENT '0.0000 to 1.0000',
    confidence_score DECIMAL(5, 4) NOT NULL COMMENT '0.0000 to 1.0000',
    suggested_action ENUM(
        'AUTO_REFUND_CUSTOMER',
        'FORCE_SETTLE_MERCHANT',
        'RESEND_WEBHOOK',
        'MANUAL_BANK_ESCALATION',
        'NO_ACTION_REQUIRED',
        'HOLD_SETTLEMENT'
    ) NOT NULL,
    feature_snapshot JSON NULL COMMENT 'Exact ML features vector passed during inference',
    model_explanation JSON NULL COMMENT 'Feature importance / SHAP values / rule breakdown',
    gemini_explanation TEXT NULL COMMENT 'Advisory explanation synthesized by Gemini',
    assessed_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (incident_id) REFERENCES incident_cases(incident_id) ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_mla_incident (incident_id),
    INDEX idx_mla_payment (payment_id),
    INDEX idx_mla_anomaly (anomaly_score),
    INDEX idx_mla_confidence (confidence_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 12. RESOLUTIONS
-- Authoritative, binding dispute and incident resolution records.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resolutions (
    resolution_id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL UNIQUE,
    payment_id VARCHAR(64) NOT NULL,
    action_taken ENUM(
        'CUSTOMER_REFUNDED',
        'MERCHANT_CREDITED',
        'WEBHOOK_RESENT_AND_FULFILLED',
        'DUPLICATE_REVERSED',
        'TRANSACTION_SETTLED_MANUALLY',
        'NO_DISCREPANCY_FOUND',
        'ESCALATED_LEGAL_COMPLIANCE'
    ) NOT NULL,
    resolution_type ENUM(
        'AUTOMATED_RULE_ENGINE',
        'ML_SUPERVISED_AUTO',
        'OPERATOR_MANUAL_OVERRIDE'
    ) NOT NULL,
    resolved_by VARCHAR(64) NOT NULL,
    resolution_notes TEXT NOT NULL,
    financial_impact_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    liability_party ENUM(
        'BANK',
        'GATEWAY',
        'MERCHANT',
        'CUSTOMER',
        'PLATFORM_LOSS_NONE'
    ) NOT NULL DEFAULT 'PLATFORM_LOSS_NONE',
    resolved_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    FOREIGN KEY (incident_id) REFERENCES incident_cases(incident_id) ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    INDEX idx_res_incident (incident_id),
    INDEX idx_res_payment (payment_id),
    INDEX idx_res_action (action_taken),
    INDEX idx_res_type (resolution_type),
    INDEX idx_res_resolved_at (resolved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 13. AUDIT_EVENTS
-- Cryptographically linked audit ledger with SHA-256 tamper-evident chaining.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    sequence_number BIGINT NOT NULL,
    entity_name VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_type ENUM(
        'SYSTEM',
        'WORKFLOW_ENGINE',
        'ML_SERVICE',
        'OPERATOR_USER',
        'BANK_CALLBACK_HANDLER'
    ) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    previous_state JSON NULL,
    new_state JSON NULL,
    ip_address VARCHAR(45) NULL,
    previous_event_hash VARCHAR(64) NOT NULL,
    current_event_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    INDEX idx_aud_entity (entity_name, entity_id),
    INDEX idx_aud_seq (sequence_number),
    INDEX idx_aud_action (action),
    INDEX idx_aud_actor (actor_type, actor_id),
    INDEX idx_aud_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
