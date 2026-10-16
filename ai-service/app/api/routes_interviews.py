from fastapi import APIRouter, HTTPException
from app.agents.interview_agent import InterviewAgent
from app.schemas.interview import InterviewAnswerRequest, InterviewGenerateRequest, InterviewSummaryRequest

router = APIRouter(prefix="/interviews", tags=["interviews"])


@router.post("/generate")
def generate_interview(request: InterviewGenerateRequest) -> dict:
    return InterviewAgent().generate(request.cv_text, request.job_description, request.question_count)


@router.post("/evaluate")
def evaluate_answer(request: InterviewAnswerRequest) -> dict:
    if not request.answer.strip():
        raise HTTPException(status_code=422, detail="An answer is required")
    return InterviewAgent().evaluate(request.question, request.answer, request.cv_text, request.job_description, request.question_type)


@router.post("/summary")
def summarize_interview(request: InterviewSummaryRequest) -> dict:
    return InterviewAgent.summarize(request.evaluations)
