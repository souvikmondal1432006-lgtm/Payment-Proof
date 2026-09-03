"""
Pydantic Schemas for Prediction & Inference Output
"""

from typing import Dict, List, Optional
from pydantic import BaseModel, Field

class ContributingSignal(BaseModel):
    signal_name: str = Field(..., description="Feature or interaction signal name")
    signal_value: str = Field(..., description="Observed value for this feature")
    importance_weight: float = Field(..., description="Estimated feature contribution score")
    interpretation: str = Field(..., description="Human-readable explanation of why this signal matters")

class IncidentPredictionResponse(BaseModel):
    payment_id: Optional[str] = Field(None, description="Payment transaction ID")
    classification: str = Field(..., description="Predicted payment incident class")
    predicted_root_cause: Optional[str] = Field(None, description="Alias for Java backend DTO")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Model confidence score")
    confidence_score: Optional[float] = Field(None, ge=0.0, le=1.0, description="Alias for Java backend DTO")
    anomaly_score: float = Field(..., ge=0.0, le=1.0, description="Divergence anomaly score")
    model_version: str = Field(..., description="Model artifact identifier and version")
    top_contributing_signals: List[ContributingSignal] = Field(default_factory=list, description="Top influential signals")
    class_probabilities: Dict[str, float] = Field(default_factory=dict, description="Probability distribution across all 9 classes")
    is_retry_prohibited_recommendation: bool = Field(..., description="Safety invariant recommendation on whether retry should be blocked")
    recommended_action: str = Field(..., description="Suggested remediation action for Java workflow engine")
    suggested_action: Optional[str] = Field(None, description="Alias for Java backend DTO")
    model_explanation: Optional[str] = Field(None, description="Explanation summary string")

    def model_post_init(self, __context):
        if self.predicted_root_cause is None:
            self.predicted_root_cause = self.classification
        if self.confidence_score is None:
            self.confidence_score = self.confidence
        if self.suggested_action is None:
            self.suggested_action = self.recommended_action
        if self.model_explanation is None and self.top_contributing_signals:
            self.model_explanation = "; ".join(s.interpretation for s in self.top_contributing_signals[:2])
