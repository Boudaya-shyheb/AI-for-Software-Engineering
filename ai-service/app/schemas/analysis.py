from pydantic import BaseModel, Field

class AnalysisRequest(BaseModel):
    cv_text: str = Field(min_length=1)
    job_description: str = Field(min_length=1)

class AnalysisResponse(BaseModel):
    analysis_id: str
    status: str
    result: dict | None = None
