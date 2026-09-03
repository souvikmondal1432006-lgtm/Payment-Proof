"""
Application Settings & Configuration
"""

from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Payment Proof ML Incident Engine"
    API_V1_STR: str = "/api"
    MODEL_VERSION: str = "incident-classifier-v1.0.0-rf"
    MODELS_DIR: str = "models"
    DATA_DIR: str = "data"
    EVAL_DIR: str = "evaluation"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = False

    class Config:
        case_sensitive = True
        env_file = ".env"

settings = Settings()
