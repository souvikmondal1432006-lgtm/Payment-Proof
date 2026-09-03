"""
FastAPI Route Handlers for Inference, Health, and Model Metadata
"""

from fastapi import APIRouter, HTTPException, status
from app.schemas.features import PaymentIncidentFeatures
from app.schemas.prediction import IncidentPredictionResponse
from app.services.inference import inference_engine

router = APIRouter()

@router.post(
    "/classify",
    response_model=IncidentPredictionResponse,
    status_code=status.HTTP_200_OK,
    summary="Classify Payment Incident",
    description="Evaluates multi-party payment telemetry and returns predicted incident class, confidence, anomaly score, contributing signals, and retry safety recommendation."
)
def classify_payment_incident(features: PaymentIncidentFeatures) -> IncidentPredictionResponse:
    try:
        return inference_engine.predict(features)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference error: {str(e)}"
        )

@router.post(
    "/predict",
    response_model=IncidentPredictionResponse,
    status_code=status.HTTP_200_OK,
    include_in_schema=False
)
def predict_alias(features: PaymentIncidentFeatures) -> IncidentPredictionResponse:
    return classify_payment_incident(features)

@router.get(
    "/health",
    status_code=status.HTTP_200_OK,
    summary="Service Health Check"
)
def health_check():
    return {
        "status": "HEALTHY",
        "service": "payment-proof-ml-engine",
        "model_loaded": inference_engine.model is not None,
        "classes_count": len(inference_engine.classes)
    }

@router.get(
    "/model/info",
    status_code=status.HTTP_200_OK,
    summary="Model Architecture & Metadata"
)
def get_model_info():
    return inference_engine.metadata if inference_engine.metadata else {
        "model_version": "incident-classifier-v1.0.0-rf",
        "status": "initialized"
    }
