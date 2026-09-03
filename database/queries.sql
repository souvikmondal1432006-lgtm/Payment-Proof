-- =============================================================================
-- PAYMENT PROOF: Forensic Investigation & Reconciliation Queries
-- Real-world investigation queries for payment contradiction triage,
-- automated dispute reconciliation, ML feature extraction, and auditing.
-- =============================================================================

-- =============================================================================
-- 1. CONTRADICTION DETECTION & TRIAGE QUERIES
-- =============================================================================

-- 1.1 Ghost Debit Detection (Bank debited customer funds, but Gateway is Failed or Pending)
-- Identifies customer funds at immediate risk where merchant cart was cancelled or expired.
SELECT 
    p.payment_id,
    p.merchant_id,
    p.order_id,
    p.amount,
    p.payment_method,
    b.bank_name,
    b.bank_status,
    b.utr_number,
    b.response_code AS bank_response_code,
    g.gateway_name,
    g.gateway_status,
    g.error_code AS gateway_error_code,
    m.order_status AS merchant_order_status,
    m.cancellation_reason
FROM payments p
JOIN bank_records b ON p.payment_id = b.payment_id
JOIN gateway_records g ON p.payment_id = g.payment_id
JOIN merchant_order_records m ON p.payment_id = m.payment_id
WHERE b.bank_status IN ('SUCCESS', 'DEBITED')
  AND g.gateway_status IN ('FAILED', 'PENDING', 'TIMED_OUT')
ORDER BY p.initiated_at DESC;

-- 1.2 Dropped Webhook Detection (Gateway captured payment, but Webhook failed/dropped)
-- Identifies orders where customer was successfully charged but merchant never fulfilled goods.
SELECT 
    p.payment_id,
    p.merchant_id,
    p.order_id,
    p.amount,
    g.gateway_name,
    g.gateway_status,
    g.capture_status,
    w.delivery_status AS webhook_status,
    w.http_status_code AS webhook_http_code,
    w.attempt_count,
    w.target_url,
    m.order_status AS merchant_status,
    m.fulfillment_status
FROM payments p
JOIN gateway_records g ON p.payment_id = g.payment_id
JOIN webhook_records w ON p.payment_id = w.payment_id
JOIN merchant_order_records m ON p.payment_id = m.payment_id
WHERE g.capture_status = 'CAPTURED'
  AND w.delivery_status IN ('FAILED', 'DROPPED', 'TIMED_OUT', 'RETRYING')
ORDER BY p.initiated_at DESC;

-- 1.3 Premature Merchant Inventory Cancellation (Merchant cancelled before 3DS auth finished)
-- Customer completed 3DS authentication after merchant's tight session expiry window.
SELECT 
    p.payment_id,
    p.order_id,
    p.amount,
    m.order_status AS merchant_status,
    m.merchant_updated_at AS merchant_cancelled_at,
    g.gateway_status,
    g.gateway_timestamp AS gateway_captured_at,
    b.bank_status,
    b.bank_timestamp AS bank_debited_at,
    TIMESTAMPDIFF(SECOND, m.merchant_updated_at, g.gateway_timestamp) AS lag_seconds_post_cancel
FROM payments p
JOIN merchant_order_records m ON p.payment_id = m.payment_id
JOIN gateway_records g ON p.payment_id = g.payment_id
JOIN bank_records b ON p.payment_id = b.payment_id
WHERE m.order_status = 'CANCELLED'
  AND g.capture_status = 'CAPTURED'
  AND g.gateway_timestamp > m.merchant_updated_at;

-- 1.4 Duplicate Payment Detection (Multiple successful charges for identical merchant order ID)
SELECT 
    p.order_id,
    COUNT(p.payment_id) AS total_payment_attempts,
    SUM(CASE WHEN b.bank_status IN ('SUCCESS', 'DEBITED') THEN 1 ELSE 0 END) AS successful_bank_debits,
    SUM(p.amount) AS total_debited_inr,
    GROUP_CONCAT(p.payment_id ORDER BY p.initiated_at) AS payment_ids,
    GROUP_CONCAT(b.utr_number ORDER BY p.initiated_at) AS utr_list
FROM payments p
JOIN bank_records b ON p.payment_id = b.payment_id
GROUP BY p.order_id
HAVING successful_bank_debits > 1;

-- =============================================================================
-- 2. 360-DEGREE FORENSIC DOSSIER QUERY
-- =============================================================================

-- 2.1 Complete Forensic Dossier for a Given Payment ID
-- Pulls the complete multi-party picture across all 13 tables for deep forensic inspection.
SELECT 
    p.payment_id,
    p.order_id,
    p.merchant_id,
    p.customer_id,
    p.amount,
    p.payment_method,
    p.status AS payment_status,
    p.initiated_at,
    
    -- Bank Perspective
    b.bank_name,
    b.bank_status,
    b.utr_number,
    b.response_code AS bank_code,
    b.network_latency_ms AS bank_latency,
    
    -- Gateway Perspective
    g.gateway_name,
    g.gateway_status,
    g.capture_status,
    g.error_code AS gateway_error,
    g.processing_latency_ms AS gateway_latency,
    
    -- Merchant OMS Perspective
    m.order_status AS merchant_status,
    m.fulfillment_status,
    m.cancellation_reason,
    
    -- Webhook Perspective
    w.delivery_status AS webhook_status,
    w.http_status_code AS webhook_code,
    w.attempt_count AS webhook_retries,
    
    -- Settlement Perspective
    s.settlement_status,
    s.net_settled_amount,
    
    -- Refund Perspective
    r.refund_status,
    r.bank_reversal_status,
    
    -- Incident & ML Perspective
    i.incident_id,
    i.incident_type,
    i.severity AS incident_severity,
    i.case_status AS incident_status,
    mla.predicted_root_cause,
    mla.anomaly_score,
    mla.confidence_score,
    mla.suggested_action,
    
    -- Resolution
    res.action_taken AS resolution_action,
    res.resolved_by,
    res.liability_party
FROM payments p
LEFT JOIN bank_records b ON p.payment_id = b.payment_id
LEFT JOIN gateway_records g ON p.payment_id = g.payment_id
LEFT JOIN merchant_order_records m ON p.payment_id = m.payment_id
LEFT JOIN webhook_records w ON p.payment_id = w.payment_id
LEFT JOIN settlement_records s ON p.payment_id = s.payment_id
LEFT JOIN refund_records r ON p.payment_id = r.payment_id
LEFT JOIN incident_cases i ON p.payment_id = i.payment_id
LEFT JOIN ml_assessments mla ON i.incident_id = mla.incident_id
LEFT JOIN resolutions res ON i.incident_id = res.incident_id
WHERE p.payment_id = 'pay_000001';

-- =============================================================================
-- 3. FINANCIAL RECONCILIATION & SETTLEMENT DISCREPANCIES
-- =============================================================================

-- 3.1 Payout Reconciliation Mismatch (Gross Amount - MDR Fee - Tax != Net Settled)
SELECT 
    s.settlement_id,
    s.payment_id,
    s.merchant_id,
    s.batch_id,
    s.gross_amount,
    s.fee_deducted,
    s.tax_deducted,
    (s.gross_amount - s.fee_deducted - s.tax_deducted) AS calculated_net_expected,
    s.net_settled_amount AS actual_net_settled,
    ROUND((s.gross_amount - s.fee_deducted - s.tax_deducted) - s.net_settled_amount, 2) AS variance_inr,
    s.settlement_status
FROM settlement_records s
WHERE ABS((s.gross_amount - s.fee_deducted - s.tax_deducted) - s.net_settled_amount) > 0.01
   OR s.settlement_status = 'DISCREPANCY';

-- 3.2 Total Settlement Funds on Hold by Merchant
SELECT 
    s.merchant_id,
    COUNT(s.settlement_id) AS blocked_transactions_count,
    SUM(s.gross_amount) AS total_blocked_gross_inr,
    SUM(s.net_settled_amount) AS total_blocked_net_inr
FROM settlement_records s
WHERE s.settlement_status = 'ON_HOLD'
GROUP BY s.merchant_id
ORDER BY total_blocked_gross_inr DESC;

-- =============================================================================
-- 4. ML FEATURE EXTRACTION DATASET EXPORT
-- =============================================================================

-- 4.1 Tabular Training Vector for Root Cause Prediction & Anomaly Classification
SELECT 
    p.payment_id,
    p.amount,
    p.payment_method,
    p.payment_method_subtype,
    b.bank_name,
    b.bank_status,
    b.network_latency_ms AS bank_latency,
    b.response_code AS bank_code,
    g.gateway_name,
    g.gateway_status,
    g.auth_status,
    g.capture_status,
    g.processing_latency_ms AS gateway_latency,
    (g.processing_latency_ms - b.network_latency_ms) AS gateway_bank_lag_ms,
    m.order_status AS merchant_status,
    m.fulfillment_status,
    w.delivery_status AS webhook_status,
    w.http_status_code AS webhook_http_code,
    w.attempt_count AS webhook_attempts,
    -- Ground truth target label for ML classification:
    i.incident_type AS target_incident_class,
    mla.anomaly_score,
    mla.confidence_score
FROM payments p
JOIN bank_records b ON p.payment_id = b.payment_id
JOIN gateway_records g ON p.payment_id = g.payment_id
JOIN merchant_order_records m ON p.payment_id = m.payment_id
LEFT JOIN webhook_records w ON p.payment_id = w.payment_id
LEFT JOIN incident_cases i ON p.payment_id = i.payment_id
LEFT JOIN ml_assessments mla ON i.incident_id = mla.incident_id;

-- =============================================================================
-- 5. OPERATOR INVESTIGATION QUEUE & SLA REPORTING
-- =============================================================================

-- 5.1 Open Critical & High Severity Incidents Requiring Human Attention
SELECT 
    i.incident_id,
    i.incident_type,
    i.severity,
    i.case_status,
    i.trigger_source,
    i.assigned_investigator,
    i.title,
    p.amount,
    p.payment_method,
    mla.predicted_root_cause,
    mla.anomaly_score,
    mla.suggested_action,
    i.opened_at,
    TIMESTAMPDIFF(MINUTE, i.opened_at, CURRENT_TIMESTAMP) AS elapsed_minutes
FROM incident_cases i
JOIN payments p ON i.payment_id = p.payment_id
LEFT JOIN ml_assessments mla ON i.incident_id = mla.incident_id
WHERE i.case_status IN ('OPEN', 'IN_REVIEW', 'ESCALATED_TO_BANK')
ORDER BY 
    CASE i.severity 
        WHEN 'CRITICAL' THEN 1 
        WHEN 'HIGH' THEN 2 
        WHEN 'MEDIUM' THEN 3 
        ELSE 4 
    END,
    i.opened_at ASC;

-- 5.2 Resolution Breakdown by Liability Party and Action Taken
SELECT 
    r.liability_party,
    r.action_taken,
    r.resolution_type,
    COUNT(r.resolution_id) AS total_cases_resolved,
    SUM(r.financial_impact_amount) AS total_financial_volume_inr
FROM resolutions r
GROUP BY r.liability_party, r.action_taken, r.resolution_type
ORDER BY total_financial_volume_inr DESC;

-- =============================================================================
-- 6. IMMUTABLE AUDIT TRAIL QUERY
-- =============================================================================

-- 6.1 Chronological Audit Timeline for Any Payment or Incident Entity
SELECT 
    a.audit_id,
    a.entity_name,
    a.entity_id,
    a.action,
    a.actor_type,
    a.actor_id,
    a.previous_state,
    a.new_state,
    a.ip_address,
    a.created_at
FROM audit_events a
WHERE a.entity_id IN ('pay_000001', 'inc_0000001')
   OR a.entity_name = 'PAYMENTS' AND a.entity_id = 'pay_000001'
ORDER BY a.created_at ASC;
