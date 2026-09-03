"""
FastAPI Application Entrypoint for Payment Proof ML Engine
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import router as api_router
from app.core.config import settings

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.MODEL_VERSION,
    description="Real-time Machine Learning Incident Classification Engine for Payment Inconsistencies.",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware allowing Java backend and frontend orchestrators
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

# Register API Router
app.include_router(api_router, prefix=settings.API_V1_STR)
app.include_router(api_router)  # Also expose /classify at root level

@app.get("/", tags=["Root"])
def read_root():
    return {
        "service": settings.PROJECT_NAME,
        "version": settings.MODEL_VERSION,
        "docs": "/docs",
        "health": "/api/health"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
