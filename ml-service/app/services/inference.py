"""
Real-time Inference Engine for Payment Incident Classification
Loads trained scikit-learn model artifacts and computes predictions,
confidence scores, anomaly metrics, and safety recommendations.
"""

import os
import json
import joblib
import pandas as pd
import numpy as np
from typing import Dict, Any, Optional

from app.schemas.features import PaymentIncidentFeatures
from app.schemas.prediction import IncidentPredictionResponse
from app.services.explainability import extract_top_signals

RECOMMENDED_ACTIONS = {
    "NORMAL": "NO_ACTION_REQUIRED",
    "DELAYED_CONFIRMATION": "NO_ACTION_REQUIRED",
    "BANK_DEBIT_GATEWAY_FAILURE": "AUTO_REFUND_CUSTOMER",
    "MISSING_WEBHOOK": "RESEND_WEBHOOK",
    "DUPLICATE_PAYMENT": "AUTO_REFUND_CUSTOMER",
    "REFUND_UNCERTAINTY": "MANUAL_BANK_ESCALATION",
    "SETTLEMENT_MISMATCH": "FORCE_SETTLE_MERCHANT",
    "ORDER_PAYMENT_CONFLICT": "AUTO_REFUND_CUSTOMER",
    "UNRESOLVED": "MANUAL_BANK_ESCALATION"
}

class MLInferenceEngine:
    def __init__(self, models_dir: Optional[str] = None):
        if models_dir is None:
            current_file_dir = os.path.dirname(os.path.abspath(__file__))
            ml_service_dir = os.path.dirname(os.path.dirname(current_file_dir))
            candidate_dir = os.path.join(ml_service_dir, "models")
            if os.path.exists(candidate_dir):
                models_dir = candidate_dir
            elif os.path.exists("models"):
                models_dir = "models"
            elif os.path.exists(os.path.join("ml-service", "models")):
                models_dir = os.path.join("ml-service", "models")
            else:
                models_dir = "models"

        self.models_dir = models_dir
        self.model = None
        self.preprocessor = None
        self.metadata = None
        self.classes = []
        self._load_artifacts()

    def _load_artifacts(self):
        model_path = os.path.join(self.models_dir, "incident_classifier_v1.joblib")
        preprocessor_path = os.path.join(self.models_dir, "preprocessor.joblib")
        metadata_path = os.path.join(self.models_dir, "model_metadata.json")

        if os.path.exists(model_path) and os.path.exists(preprocessor_path):
            self.model = joblib.load(model_path)
            self.preprocessor = joblib.load(preprocessor_path)
            if hasattr(self.model, "classes_"):
                self.classes = list(self.model.classes_)
            print(f"[ML Engine] Successfully loaded ML model from {model_path} with {len(self.classes)} classes: {self.classes}")

        if os.path.exists(metadata_path):
            with open(metadata_path, "r") as f:
                self.metadata = json.load(f)
        else:
            self.metadata = {
                "model_version": "incident-classifier-v1.0.0-rf",
                "classes": self.classes
            }

    def predict(self, features: PaymentIncidentFeatures) -> IncidentPredictionResponse:
        features_dict = features.model_dump()
        df = pd.DataFrame([features_dict])

        # If model artifacts are available, run scikit-learn model
        if self.model is not None and self.preprocessor is not None:
            X_transformed = self.preprocessor.transform(df)
            probs = self.model.predict_proba(X_transformed)[0]
            pred_idx = np.argmax(probs)
            pred_class = self.classes[pred_idx]
            confidence = float(probs[pred_idx])

            prob_dict = {
                cls_name: round(float(probs[i]), 4)
                for i, cls_name in enumerate(self.classes)
            }
        else:
            # Fallback heuristic if artifacts not yet built
            pred_class = "NORMAL"
            confidence = 0.95
            prob_dict = {"NORMAL": 0.95}

        # Calculate Anomaly Score (1.0 - probability of NORMAL)
        normal_prob = prob_dict.get("NORMAL", 0.0)
        anomaly_score = round(float(1.0 - normal_prob), 4)

        # Prohibit-Retry Safety Invariant
        # Backend must NEVER blindly retry if funds were debited during an active incident or ambiguous failure
        bank_status = str(features.bank_status).upper()
        gateway_status = str(features.gateway_status).upper()

        if pred_class in [
            "BANK_DEBIT_GATEWAY_FAILURE", "DUPLICATE_PAYMENT", "ORDER_PAYMENT_CONFLICT", "UNRESOLVED"
        ]:
            is_retry_prohibited = True
        elif bank_status in ["SUCCESS", "DEBITED"] and gateway_status in ["FAILED", "TIMED_OUT", "PENDING"]:
            is_retry_prohibited = True
        elif features.refund_status in ["PENDING", "MANUAL_INTERVENTION_REQUIRED"]:
            is_retry_prohibited = True
        else:
            is_retry_prohibited = False

        # Extract Top Contributing Signals
        top_signals = extract_top_signals(features_dict, pred_class, confidence)

        recommended_action = RECOMMENDED_ACTIONS.get(pred_class, "MANUAL_BANK_ESCALATION")

        # Human explanation synthesis
        explanation = f"Incident classified as {pred_class} with {confidence * 100:.1f}% confidence based on multi-party evidence."
        if top_signals:
            explanation = "; ".join([s.interpretation for s in top_signals[:2]])

        return IncidentPredictionResponse(
            payment_id=features.payment_id or "pay_unknown",
            classification=pred_class,
            predicted_root_cause=pred_class,
            confidence=round(confidence, 4),
            confidence_score=round(confidence, 4),
            anomaly_score=anomaly_score,
            model_version=self.metadata.get("model_version", "incident-classifier-v1.0.0-rf"),
            top_contributing_signals=top_signals,
            class_probabilities=prob_dict,
            is_retry_prohibited_recommendation=is_retry_prohibited,
            recommended_action=recommended_action,
            suggested_action=recommended_action,
            model_explanation=explanation
        )

# Global Singleton Instance
inference_engine = MLInferenceEngine()
