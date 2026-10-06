from pydantic import BaseModel, Field


class InterviewGenerateRequest(BaseModel):
    cv_text: str = Field(min_length=1, max_length=50000)
    job_description: str = Field(min_length=1, max_length=30000)
    question_count: int = Field(default=8, ge=4, le=12)


class InterviewAnswerRequest(BaseModel):
    question: str = Field(min_length=1, max_length=2000)
    answer: str = Field(min_length=1, max_length=10000)
    cv_text: str = Field(min_length=1, max_length=50000)
    job_description: str = Field(min_length=1, max_length=30000)
    question_type: str = Field(default="behavioral", max_length=30)


class InterviewSummaryRequest(BaseModel):
    evaluations: list[dict] = Field(min_length=1, max_length=12)
