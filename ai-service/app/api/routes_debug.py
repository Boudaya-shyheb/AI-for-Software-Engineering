from fastapi import APIRouter

from app.agents.code_debug_agent import CodeDebugAgent
from app.schemas.debug import CodeDebugRequest

router = APIRouter(prefix="/code-debug", tags=["code-debug"])


@router.post("")
def debug_code(request: CodeDebugRequest) -> dict:
    return CodeDebugAgent().analyze(request.module, request.language, request.code, request.expected_behavior)
