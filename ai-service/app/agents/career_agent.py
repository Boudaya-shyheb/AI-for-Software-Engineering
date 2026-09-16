from uuid import uuid4
from app.tools.embedding_tool import calculate_embedding_similarity
from app.tools.preprocessing_tool import preprocess_cv_text
from app.tools.scoring_tool import ScoreInput, calculate_compatibility_score, calculate_education_score, calculate_experience_score
from app.tools.skill_matching_tool import compare_skills
from app.tools.tfidf_tool import calculate_tfidf_similarity

class CareerAgent:
    def analyze(self, cv_text: str, job_description: str) -> dict:
        if not cv_text or not cv_text.strip():
            raise ValueError("CV text is required")
        if not job_description or not job_description.strip():
            raise ValueError("Job description is required")
        processed = preprocess_cv_text(cv_text)
        candidate_skills = self._extract_terms(cv_text)
        required_skills = self._extract_terms(job_description)
        tfidf = calculate_tfidf_similarity(processed.text, job_description)
        embeddings = calculate_embedding_similarity(processed.text, job_description)
        skill_result = compare_skills(candidate_skills, required_skills, [])
        skills_score = len(skill_result["matching_skills"]) / max(1, len(required_skills))
        experience = calculate_experience_score([], [])
        education = calculate_education_score([], [])
        score = calculate_compatibility_score(ScoreInput(skills_score=skills_score, semantic_score=embeddings["similarity"], experience_score=experience["score"], education_score=education["score"]))
        return {"analysis_id": str(uuid4()), "compatibility_score": score["final_score"], "models": {"tfidf": tfidf, "embeddings": embeddings}, "skills": skill_result, "experience": experience, "education": education, "ai_explanation": {"summary": "Analyse calculée à partir des éléments fournis.", "strengths": skill_result["matching_skills"], "skill_gaps": skill_result["missing_required_skills"], "experience_analysis": "Analyse déterministe de l'expérience disponible.", "recommendations": [], "cv_improvements": []}, "status": "COMPLETED"}

    @staticmethod
    def _extract_terms(text: str) -> list[str]:
        terms = []
        for token in text.replace(",", " ").split():
            clean = token.strip(".;:()[]").lower()
            if len(clean) > 2 and clean not in {"the", "and", "with", "for", "les", "des", "une", "dans"}:
                terms.append(token.strip(".;:()[]"))
        return list(dict.fromkeys(terms))
