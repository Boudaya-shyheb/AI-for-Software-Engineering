from fastapi import FastAPI
from app.api.routes_analysis import router as analysis_router

app = FastAPI(title="AI Career Agent", version="0.1.0")
app.include_router(analysis_router, prefix="/api")

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai-service"}
