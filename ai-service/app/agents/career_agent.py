import re
from uuid import uuid4

from app.tools.embedding_tool import calculate_embedding_similarity
from app.tools.preprocessing_tool import preprocess_cv_text
from app.tools.scoring_tool import ScoreInput, calculate_compatibility_score
from app.tools.skill_matching_tool import compare_skills
from app.tools.tfidf_tool import calculate_tfidf_similarity

SKILL_ALIASES = {
    "angular": ["angular"], "react": ["react", "reactjs"], "javascript": ["javascript", "js"],
    "typescript": ["typescript", "ts"], "java": ["java"], "spring boot": ["spring boot", "springboot"],
    "python": ["python"], "fastapi": ["fastapi"], "node.js": ["node.js", "nodejs"],
    "postgresql": ["postgresql", "postgres"], "mysql": ["mysql"], "docker": ["docker"],
    "kubernetes": ["kubernetes", "k8s"], "git": ["git"], "ci/cd": ["ci/cd", "ci cd"],
    "rest api": ["rest api", "api rest", "restful"], "microservices": ["microservices"],
    "aws": ["aws", "amazon web services"], "azure": ["azure"],
    "machine learning": ["machine learning"], "teamwork": ["teamwork", "travail en équipe", "esprit d'équipe"],
}

class CareerAgent:
    def analyze(self, cv_text: str, job_description: str) -> dict:
        if not cv_text or not cv_text.strip(): raise ValueError("CV text is required")
        if not job_description or not job_description.strip(): raise ValueError("Job description is required")
        processed = preprocess_cv_text(cv_text)
        candidate_skills = self._extract_skills(cv_text) or self._extract_terms(cv_text)
        required_skills = self._extract_skills(job_description) or self._extract_terms(job_description)
        tfidf = calculate_tfidf_similarity(processed.text, job_description)
        embeddings = calculate_embedding_similarity(processed.text, job_description)
        skill_result = compare_skills(candidate_skills, required_skills, [])
        skills_score = len(skill_result["matching_skills"]) / max(1, len(required_skills))
        candidate_years, required_years = self._extract_years(cv_text), self._extract_years(job_description)
        experience_score = 1.0 if required_years == 0 else min(1.0, candidate_years / required_years)
        degree_pattern = r"\b(degree|bachelor|master|licence|ingénieur|diplôme)\b"
        degree_required = bool(re.search(degree_pattern, job_description, re.I))
        degree_found = bool(re.search(degree_pattern, cv_text, re.I))
        education_score = 1.0 if not degree_required else float(degree_found)
        score = calculate_compatibility_score(ScoreInput(skills_score=skills_score, semantic_score=embeddings["similarity"], experience_score=experience_score, education_score=education_score))
        missing = skill_result["missing_required_skills"][:6]
        recommendations = [f"Strengthen or demonstrate your experience with {skill}." for skill in missing[:4]]
        if required_years > candidate_years: recommendations.append(f"Clarify transferable experience toward the {required_years:g}-year requirement.")
        if not recommendations: recommendations.append("Add measurable achievements for the strongest matching skills.")
        improvements = []
        if "experience" not in processed.sections: improvements.append("Add a clearly titled Experience section with dates and outcomes.")
        if "skills" not in processed.sections: improvements.append("Add a dedicated Skills section using the vocabulary of the role.")
        if len(cv_text) < 400: improvements.append("Provide more detail about projects, responsibilities, and measurable results.")
        evidence = min(1.0, len(candidate_skills) / 8 * .6 + min(len(cv_text) / 1200, 1) * .4)
        confidence = round(.45 + evidence * .5, 3)
        summary = (f"Your profile matches {len(skill_result['matching_skills'])} of {len(required_skills)} detected role skills. Focus first on {', '.join(missing[:3])}." if missing else "Your detected core skills align well with this role; strengthen the application with measurable achievements.")
        return {
            "analysis_id": str(uuid4()), "compatibility_score": score["final_score"], "confidence": confidence,
            "score_breakdown": {"skills": round(skills_score, 6), "semantic": embeddings["similarity"], "experience": round(experience_score, 6), "education": education_score},
            "models": {"tfidf": tfidf, "embeddings": embeddings}, "skills": skill_result,
            "profile": {"skills": candidate_skills, "years_experience": candidate_years, "education_detected": degree_found, "languages": self._extract_languages(cv_text), "certifications": self._extract_certifications(cv_text)},
            "experience": {"score": round(experience_score, 6), "years_detected": candidate_years, "years_required": required_years},
            "education": {"score": education_score, "required": degree_required, "detected": degree_found},
            "ai_explanation": {"summary": summary, "strengths": skill_result["matching_skills"], "skill_gaps": missing, "experience_analysis": f"Detected {candidate_years:g} years; role asks for {required_years:g} years.", "recommendations": recommendations, "cv_improvements": improvements},
            "status": "COMPLETED",
        }

    @staticmethod
    def _extract_skills(text):
        lowered = text.lower()
        return [name for name, aliases in SKILL_ALIASES.items() if any(alias in lowered for alias in aliases)]

    @staticmethod
    def _extract_years(text):
        values = [float(value.replace(",", ".")) for value in re.findall(r"(\d+(?:[.,]\d+)?)\s*(?:years?|ans?|années?)", text, re.I)]
        return max(values, default=0.0)

    @staticmethod
    def _extract_languages(text):
        aliases = {"French": ["french", "français"], "English": ["english", "anglais"], "Arabic": ["arabic", "arabe"], "Spanish": ["spanish", "espagnol"], "German": ["german", "allemand"]}
        lowered = text.lower()
        return [name for name, terms in aliases.items() if any(term in lowered for term in terms)]

    @staticmethod
    def _extract_certifications(text):
        return [value.strip() for value in re.findall(r"(?:certification|certified|certifié)[\s:-]+([^\n,;]{2,60})", text, re.I)[:8]]

    @staticmethod
    def _extract_terms(text):
        ignored = {"the", "and", "with", "for", "les", "des", "une", "dans"}
        terms = [token.strip(".;:()[]") for token in text.replace(",", " ").split()]
        return list(dict.fromkeys(token for token in terms if len(token) > 2 and token.lower() not in ignored))
