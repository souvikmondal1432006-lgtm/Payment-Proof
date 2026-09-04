"""
Integration Tests for FastAPI HTTP Endpoints
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health_check_endpoint():
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert data["service"] == "payment-proof-ml-engine"

def test_model_info_endpoint():
    response = client.get("/api/model/info")
    assert response.status_code == 200
    data = response.json()
    assert "model_version" in data

def test_classify_endpoint_success():
    payload = {
        "payment_id": "pay_test_api_01",
        "amount": 2500.0,
        "payment_method": "UPI",
        "bank": "HDFC",
        "gateway": "RAZORPAY",
        "bank_status": "SUCCESS",
        "gateway_status": "SUCCESS",
        "auth_status": "AUTHORIZED",
        "capture_status": "CAPTURED",
        "merchant_order_status": "PAID",
        "merchant_fulfillment_status": "DELIVERED",
        "webhook_status": "DELIVERED",
        "webhook_http_code": 200,
        "settlement_status": "SETTLED"
    }

    response = client.post("/api/classify", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["payment_id"] == "pay_test_api_01"
    assert "classification" in data
    assert "confidence" in data
    assert "anomaly_score" in data
    assert "top_contributing_signals" in data
    assert "recommended_action" not in data
    assert "is_retry_prohibited_recommendation" not in data

def test_classify_invalid_amount():
    payload = {
        "payment_id": "pay_test_invalid",
        "amount": -50.0  # Invalid negative amount
    }

    response = client.post("/api/classify", json=payload)
    assert response.status_code == 422  # Pydantic validation error
