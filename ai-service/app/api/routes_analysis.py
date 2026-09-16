from fastapi import APIRouter, HTTPException
from app.agents.career_agent import CareerAgent
from app.schemas.analysis import AnalysisRequest, AnalysisResponse

router = APIRouter(tags=["analysis"])

@router.post("/analyses", response_model=AnalysisResponse)
def create_analysis(request: AnalysisRequest) -> AnalysisResponse:
    try:
        result = CareerAgent().analyze(request.cv_text, request.job_description)
        return AnalysisResponse(analysis_id=result["analysis_id"], status=result["status"], result=result)
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=500, detail="Analysis failed") from error
