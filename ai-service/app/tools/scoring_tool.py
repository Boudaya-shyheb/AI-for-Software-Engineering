from pydantic import BaseModel, Field

class ScoreInput(BaseModel):
    skills_score: float = Field(ge=0, le=1)
    semantic_score: float = Field(ge=0, le=1)
    experience_score: float = Field(ge=0, le=1)
    education_score: float = Field(ge=0, le=1)

WEIGHTS = {"skills": 0.40, "semantic": 0.30, "experience": 0.20, "education": 0.10}

def calculate_compatibility_score(values: ScoreInput) -> dict:
    final_score = (values.skills_score * WEIGHTS["skills"] + values.semantic_score * WEIGHTS["semantic"] + values.experience_score * WEIGHTS["experience"] + values.education_score * WEIGHTS["education"])
    return {**values.model_dump(), "final_score": round(final_score, 6), "weights": WEIGHTS}

def calculate_experience_score(candidate_experience: list[dict], required_experience: list[str]) -> dict:
    if not required_experience:
        return {"score": 1.0, "years_detected": sum(float(item.get("years", 0)) for item in candidate_experience)}
    years = sum(float(item.get("years", 0)) for item in candidate_experience)
    required_years = max([float(item.get("years", 0)) for item in candidate_experience] + [1.0])
    return {"score": round(min(1.0, years / required_years), 6), "years_detected": years}

def calculate_education_score(candidate_education: list[str], requirements: list[str]) -> dict:
    if not requirements:
        return {"score": 1.0}
    candidate_text = " ".join(candidate_education).lower()
    matches = sum(1 for requirement in requirements if requirement.lower() in candidate_text)
    return {"score": round(matches / len(requirements), 6)}
