"""
Unit Tests for Feature Engineering & Preprocessing Pipeline
"""

import pytest
import pandas as pd
import numpy as np
from training.feature_engineering import DomainSignalExtractor, build_preprocessor_pipeline

def test_domain_signal_extractor():
    sample_data = pd.DataFrame([{
        "amount": 2500.0,
        "bank_latency_ms": 500,
        "gateway_latency_ms": 800,
        "transaction_age_seconds": 300,
        "time_of_day_hour": 14,
        "retry_count": 0,
        "webhook_attempt_count": 1,
        "webhook_http_code": 200,
        "amount_deviation_score": 0.1,
        "historical_merchant_failure_rate": 0.03,
        "payment_method": "UPI",
        "bank": "HDFC",
        "gateway": "RAZORPAY",
        "bank_status": "SUCCESS",
        "gateway_status": "FAILED",
        "auth_status": "TIMEOUT",
        "capture_status": "FAILED",
        "merchant_order_status": "CANCELLED",
        "merchant_fulfillment_status": "CANCELLED",
        "webhook_status": "FAILED",
        "settlement_status": "ON_HOLD",
        "refund_status": "NONE",
        "bank_reversal_status": "NONE",
        "is_amount_matched": 1,
        "is_duplicate_candidate": 0
    }])

    extractor = DomainSignalExtractor()
    transformed_df = extractor.transform(sample_data)

    # Assert ghost debit signal is correctly triggered
    assert transformed_df["is_ghost_debit_signal"].iloc[0] == 1
    # Assert debited order conflict is triggered
    assert transformed_df["is_debited_order_conflict"].iloc[0] == 1
    # Assert latency diff calculation
    assert transformed_df["latency_diff_ms"].iloc[0] == 300

def test_preprocessor_pipeline_output_shape():
    sample_data = pd.DataFrame([{
        "amount": 1000.0,
        "bank_latency_ms": 300,
        "gateway_latency_ms": 450,
        "transaction_age_seconds": 120,
        "time_of_day_hour": 10,
        "retry_count": 0,
        "webhook_attempt_count": 1,
        "webhook_http_code": 200,
        "amount_deviation_score": 0.0,
        "historical_merchant_failure_rate": 0.04,
        "payment_method": "UPI",
        "bank": "ICICI",
        "gateway": "PAYU",
        "bank_status": "SUCCESS",
        "gateway_status": "SUCCESS",
        "auth_status": "AUTHORIZED",
        "capture_status": "CAPTURED",
        "merchant_order_status": "PAID",
        "merchant_fulfillment_status": "DELIVERED",
        "webhook_status": "DELIVERED",
        "settlement_status": "SETTLED",
        "refund_status": "NONE",
        "bank_reversal_status": "NONE",
        "is_amount_matched": 1,
        "is_duplicate_candidate": 0
    }])

    pipeline = build_preprocessor_pipeline()
    output_matrix = pipeline.fit_transform(sample_data)

    assert isinstance(output_matrix, np.ndarray)
    assert output_matrix.shape[0] == 1
    assert output_matrix.shape[1] > 10  # Encoded dimension
