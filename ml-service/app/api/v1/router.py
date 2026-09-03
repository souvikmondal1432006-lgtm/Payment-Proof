from fastapi import APIRouter, HTTPException, status
from app.schemas.incident import (
    IncidentClassificationRequest,
    IncidentClassificationResponse,
    AnomalyExplanationRequest,
    AnomalyExplanationResponse,
)
from app.services.classifier import IncidentClassificationEngine

router = APIRouter()


@router.post(
    "/classify-incident",
    response_model=IncidentClassificationResponse,
    status_code=status.HTTP_200_OK,
    summary="Classify payment state contradiction",
    description="Analyzes multi-party telemetry events and returns ML incident classification, anomaly probability, and explanation factors."
)
async def classify_incident(request: IncidentClassificationRequest) -> IncidentClassificationResponse:
    try:
        return IncidentClassificationEngine.classify(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference error processing transaction {request.transaction_id}: {str(e)}"
        )


@router.post(
    "/explain-anomaly",
    response_model=AnomalyExplanationResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate anomaly breakdown and verification steps",
    description="Provides deep anomaly analysis and suggested operator verification steps."
)
async def explain_anomaly(request: AnomalyExplanationRequest) -> AnomalyExplanationResponse:
    try:
        return IncidentClassificationEngine.explain(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Explanation generation failed: {str(e)}"
        )
