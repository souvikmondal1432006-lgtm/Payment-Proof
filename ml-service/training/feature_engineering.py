"""
Feature Engineering & Transformation Pipeline for Payment Incident Classification
Defines custom feature extractors, preprocessors, and feature transformers.
Ensures zero target leakage and strict reproducibility.
"""

import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

# Raw feature columns expected from input
NUMERICAL_RAW_COLS = [
    "amount",
    "bank_latency_ms",
    "gateway_latency_ms",
    "transaction_age_seconds",
    "time_of_day_hour",
    "retry_count",
    "webhook_attempt_count",
    "webhook_http_code",
    "amount_deviation_score",
    "historical_merchant_failure_rate"
]

CATEGORICAL_RAW_COLS = [
    "payment_method",
    "bank",
    "gateway",
    "bank_status",
    "gateway_status",
    "auth_status",
    "capture_status",
    "merchant_order_status",
    "merchant_fulfillment_status",
    "webhook_status",
    "settlement_status",
    "refund_status",
    "bank_reversal_status"
]

BINARY_RAW_COLS = [
    "is_amount_matched",
    "is_duplicate_candidate"
]

class DomainSignalExtractor(BaseEstimator, TransformerMixin):
    """
    Constructs high-signal domain features from multi-party payment telemetry.
    All logic operates exclusively on observable telemetry inputs without label knowledge.
    """
    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = pd.DataFrame(X).copy()
        
        # Fill missing values if any
        for col in NUMERICAL_RAW_COLS:
            if col not in df.columns:
                df[col] = 0.0
            else:
                df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0.0)

        for col in CATEGORICAL_RAW_COLS:
            if col not in df.columns:
                df[col] = "UNKNOWN"
            else:
                df[col] = df[col].astype(str).fillna("UNKNOWN")

        for col in BINARY_RAW_COLS:
            if col not in df.columns:
                df[col] = 0
            else:
                df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0).astype(int)

        # Domain Engineered Signals
        # 1. Latency Divergence & Ratios
        df["latency_diff_ms"] = df["gateway_latency_ms"] - df["bank_latency_ms"]
        df["is_extreme_bank_latency"] = (df["bank_latency_ms"] > 45000).astype(int)
        
        # 2. Bank vs Gateway Status Divergence (Ghost Debit)
        is_bank_debited = df["bank_status"].isin(["SUCCESS", "DEBITED"]).astype(int)
        is_gateway_not_success = df["gateway_status"].isin(["FAILED", "TIMED_OUT", "PENDING"]).astype(int)
        is_gateway_timeout = (df["gateway_latency_ms"] > 10000).astype(int)
        is_merchant_cancelled = df["merchant_order_status"].isin(["CANCELLED", "EXPIRED"]).astype(int)
        df["is_ghost_debit_signal"] = (is_bank_debited & (is_gateway_not_success | is_gateway_timeout) & is_merchant_cancelled).astype(int)

        # 3. Captured vs Dropped Webhook Divergence
        is_captured = (df["capture_status"] == "CAPTURED").astype(int)
        is_wh_dropped = df["webhook_status"].isin(["DROPPED", "FAILED", "TIMED_OUT"]).astype(int)
        df["is_captured_webhook_dropped"] = (is_captured & is_wh_dropped).astype(int)

        # 4. Debited vs Cancelled Cart Divergence
        df["is_debited_order_conflict"] = (is_bank_debited & is_merchant_cancelled).astype(int)

        # 5. Settlement Discrepancy Flag (Discrepancy status or significant MDR/amount drift)
        df["is_settlement_discrepancy"] = ((df["settlement_status"] == "DISCREPANCY") | (df["amount_deviation_score"].abs() > 0.025)).astype(int)

        # 6. Refund Stalled Flag
        is_refund_flagged = df["refund_status"].isin(["MANUAL_INTERVENTION_REQUIRED", "PENDING", "FAILED"]).astype(int)
        is_bank_rev_not_credited = (df["bank_reversal_status"] != "CREDITED_TO_CUSTOMER").astype(int)
        df["is_refund_stalled_signal"] = (is_refund_flagged & is_bank_rev_not_credited).astype(int)

        # 7. Duplicate Retry Signal
        df["is_duplicate_retry_signal"] = ((df["retry_count"] > 0) & ((df["is_duplicate_candidate"] == 1) | (df["transaction_age_seconds"] < 300))).astype(int)

        return df

def build_preprocessor_pipeline():
    """
    Builds the full sklearn ColumnTransformer pipeline combining
    DomainSignalExtractor, StandardScaler, and OneHotEncoder.
    """
    engineered_numerical_cols = NUMERICAL_RAW_COLS + [
        "latency_diff_ms"
    ]
    
    engineered_binary_cols = BINARY_RAW_COLS + [
        "is_extreme_bank_latency",
        "is_ghost_debit_signal",
        "is_captured_webhook_dropped",
        "is_debited_order_conflict",
        "is_settlement_discrepancy",
        "is_refund_stalled_signal",
        "is_duplicate_retry_signal"
    ]

    num_transformer = Pipeline(steps=[
        ("scaler", StandardScaler())
    ])

    cat_transformer = Pipeline(steps=[
        ("onehot", OneHotEncoder(handle_unknown="ignore", sparse_output=False))
    ])

    column_transformer = ColumnTransformer(
        transformers=[
            ("num", num_transformer, engineered_numerical_cols),
            ("cat", cat_transformer, CATEGORICAL_RAW_COLS),
            ("bin", "passthrough", engineered_binary_cols)
        ],
        remainder="drop"
    )

    full_pipeline = Pipeline(steps=[
        ("domain_signals", DomainSignalExtractor()),
        ("encoder", column_transformer)
    ])

    return full_pipeline
