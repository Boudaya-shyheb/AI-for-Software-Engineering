from fastapi import APIRouter, HTTPException

from app.agents.application_agent import ApplicationAgent
from app.schemas.application import ApplicationGenerateRequest

router = APIRouter(prefix="/applications", tags=["applications"])


@router.post("/generate")
def generate_application(request: ApplicationGenerateRequest) -> dict:
    try:
        return ApplicationAgent().generate(
            request.cv_text,
            request.job_title,
            request.company,
            request.job_description,
            request.candidate_name,
        )
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
