"""
Unit Tests for Inference Engine, Safety Invariants, and Explainability
"""

import pytest
from app.schemas.features import PaymentIncidentFeatures
from app.services.inference import MLInferenceEngine

def test_inference_engine_ghost_debit_safety():
    engine = MLInferenceEngine(models_dir="models")
    
    # Simulate a Ghost Debit scenario: Bank debited, gateway failed
    payload = PaymentIncidentFeatures(
        payment_id="pay_test_001",
        amount=4999.0,
        payment_method="UPI",
        bank="HDFC",
        gateway="RAZORPAY",
        bank_status="DEBITED",
        gateway_status="FAILED",
        auth_status="TIMEOUT",
        capture_status="FAILED",
        merchant_order_status="CANCELLED",
        merchant_fulfillment_status="CANCELLED",
        webhook_status="FAILED",
        bank_latency_ms=450,
        gateway_latency_ms=60000
    )

    response = engine.predict(payload)

    assert response.payment_id == "pay_test_001"
    assert response.confidence >= 0.0 and response.confidence <= 1.0
    assert response.anomaly_score >= 0.0 and response.anomaly_score <= 1.0
    assert response.classification in ["BANK_DEBIT_GATEWAY_FAILURE", "ORDER_PAYMENT_CONFLICT", "UNRESOLVED"]
    assert len(response.top_contributing_signals) > 0
    # Confirm ML service does not provide operational/financial recommendations
    assert not hasattr(response, "recommended_action") or getattr(response, "recommended_action", None) is None
    assert not hasattr(response, "is_retry_prohibited_recommendation") or getattr(response, "is_retry_prohibited_recommendation", None) is None

def test_inference_engine_normal_payment():
    engine = MLInferenceEngine(models_dir="models")

    # Simulate Normal Synchronized Payment
    payload = PaymentIncidentFeatures(
        payment_id="pay_test_002",
        amount=850.0,
        payment_method="CREDIT_CARD",
        bank="ICICI",
        gateway="RAZORPAY",
        bank_status="SUCCESS",
        gateway_status="SUCCESS",
        auth_status="AUTHORIZED",
        capture_status="CAPTURED",
        merchant_order_status="PAID",
        merchant_fulfillment_status="DELIVERED",
        webhook_status="DELIVERED",
        webhook_http_code=200,
        settlement_status="SETTLED",
        bank_latency_ms=320,
        gateway_latency_ms=450
    )

    response = engine.predict(payload)

    assert response.confidence > 0.5
    assert response.classification == "NORMAL"
    assert not hasattr(response, "recommended_action") or getattr(response, "recommended_action", None) is None
