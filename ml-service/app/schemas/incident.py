from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from enum import Enum
from datetime import datetime


class ProviderType(str, Enum):
    BANK = "BANK"
    GATEWAY = "GATEWAY"
    MERCHANT_APP = "MERCHANT_APP"
    WEBHOOK_SERVICE = "WEBHOOK_SERVICE"


class ProviderTelemetryInput(BaseModel):
    provider_type: ProviderType
    provider_name: str
    reported_status: str
    reported_amount: Optional[float] = None
    raw_response_code: Optional[str] = None
    raw_response_message: Optional[str] = None
    event_timestamp: datetime
    latency_ms: Optional[int] = None
    payload_hash: Optional[str] = None


class IncidentClassificationRequest(BaseModel):
    transaction_id: str
    reference_id: str
    amount: float
    currency: str = "INR"
    payment_method: str = "UPI"
    created_at: datetime
    telemetries: List[ProviderTelemetryInput] = Field(..., min_length=1)


class FeatureImportance(BaseModel):
    feature_name: str
    weight: float
    description: str
    evidence_value: Any


class IncidentClassificationResponse(BaseModel):
    transaction_id: str
    predicted_classification: str
    confidence_score: float = Field(..., ge=0.0, le=1.0)
    anomaly_score: float = Field(..., ge=0.0, le=1.0)
    root_cause_hypothesis: str
    recommended_action_hypothesis: str
    feature_importances: List[FeatureImportance]
    explanation_metadata: Dict[str, Any]
    analyzed_at: datetime


class AnomalyExplanationRequest(BaseModel):
    transaction_id: str
    classification: str
    telemetries: List[ProviderTelemetryInput]


class AnomalyExplanationResponse(BaseModel):
    transaction_id: str
    anomaly_type: str
    timeline_lag_seconds: float
    divergence_matrix: Dict[str, str]
    risk_level: str
    confidence_factors: List[str]
    suggested_verification_steps: List[str]


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    environment: str
    timestamp: datetime
