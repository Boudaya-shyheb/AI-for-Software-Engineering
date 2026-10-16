from pydantic import BaseModel, Field

class AnalysisRequest(BaseModel):
    cv_text: str = Field(min_length=1)
    job_description: str = Field(min_length=1)

class AnalysisResponse(BaseModel):
    analysis_id: str
    status: str
    result: dict | None = None

class JobOfferMatchInput(BaseModel):
    id: int
    title: str
    company: str
    location: str
    employmentType: str
    description: str = Field(min_length=1, max_length=20000)

class JobMatchesRequest(BaseModel):
    cv_text: str = Field(min_length=1, max_length=50000)
    offers: list[JobOfferMatchInput] = Field(max_length=5000)
