from pydantic import BaseModel, Field


class ApplicationGenerateRequest(BaseModel):
    cv_text: str = Field(min_length=1, max_length=50000)
    job_title: str = Field(min_length=1, max_length=160)
    company: str = Field(min_length=1, max_length=140)
    job_description: str = Field(min_length=1, max_length=30000)
    candidate_name: str = Field(min_length=1, max_length=100)
