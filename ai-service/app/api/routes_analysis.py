from pathlib import Path
from tempfile import NamedTemporaryFile
from fastapi import APIRouter, File, HTTPException, UploadFile
from app.agents.career_agent import CareerAgent
from app.schemas.analysis import AnalysisRequest, AnalysisResponse, JobMatchesRequest
from app.tools.pdf_tool import extract_cv_text

router = APIRouter(tags=["analysis"])

@router.post("/job-matches")
def match_job_offers(request: JobMatchesRequest) -> dict:
    try:
        return CareerAgent().match_jobs(request.cv_text, [offer.model_dump() for offer in request.offers])
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=500, detail="Job matching failed") from error

@router.post("/analyses", response_model=AnalysisResponse)
def create_analysis(request: AnalysisRequest) -> AnalysisResponse:
    try:
        result = CareerAgent().analyze(request.cv_text, request.job_description)
        return AnalysisResponse(analysis_id=result["analysis_id"], status=result["status"], result=result)
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=500, detail="Analysis failed") from error

@router.post("/documents/extract")
async def extract_document(file: UploadFile = File(...)) -> dict:
    suffix = Path(file.filename or "").suffix.lower()
    if suffix not in {".pdf", ".docx", ".txt"}:
        raise HTTPException(status_code=415, detail="Only PDF, DOCX and TXT files are supported")
    content = await file.read()
    if not content:
        raise HTTPException(status_code=422, detail="The uploaded file is empty")
    if len(content) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="The file must be smaller than 10 MB")
    temp_path: Path | None = None
    try:
        with NamedTemporaryFile(delete=False, suffix=suffix) as temporary:
            temporary.write(content)
            temp_path = Path(temporary.name)
        result = extract_cv_text(str(temp_path))
        if not result["text"].strip():
            raise HTTPException(status_code=422, detail="No readable text was found in this document")
        return {"filename": file.filename, **result}
    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(status_code=422, detail="The document could not be read") from error
    finally:
        if temp_path is not None:
            temp_path.unlink(missing_ok=True)
