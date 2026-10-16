from fastapi import FastAPI
from app.api.routes_analysis import router as analysis_router
from app.api.routes_interviews import router as interviews_router
from app.api.routes_debug import router as debug_router

app = FastAPI(title="AI Career Agent", version="0.1.0")
app.include_router(analysis_router, prefix="/api")
app.include_router(interviews_router, prefix="/api")
app.include_router(debug_router, prefix="/api")

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai-service"}
