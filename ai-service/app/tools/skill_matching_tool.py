import re
from difflib import SequenceMatcher

STOPWORDS = {"and", "the", "with", "for", "from", "years", "experience", "required", "preferred"}

def _normalize(value: str) -> str:
    return re.sub(r"[^a-z0-9+#.]", "", value.lower().replace(" ", ""))

def _tokens(value: str) -> set[str]:
    return {
        token for token in re.findall(r"[a-z0-9+#.]+", value.lower())
        if token not in STOPWORDS and len(token) >= 3
    }

def compare_skills(candidate_skills: list[str], required_skills: list[str], preferred_skills: list[str]) -> dict:
    candidate = {skill: _normalize(skill) for skill in candidate_skills}
    def match(target: str) -> str | None:
        target_norm = _normalize(target)
        best = max(candidate, key=lambda item: SequenceMatcher(None, target_norm, candidate[item]).ratio(), default=None)
        if not best:
            return None
        similarity = SequenceMatcher(None, target_norm, candidate[best]).ratio()
        shared_tokens = _tokens(target) & _tokens(best)
        token_overlap = len(shared_tokens) / max(1, min(len(_tokens(target)), len(_tokens(best))))
        if similarity >= 0.82 or token_overlap >= 0.5:
            return best
        return None
    matching = []
    missing_required = []
    for skill in required_skills:
        found = match(skill)
        if found:
            matching.append({"candidate_skill": found, "job_skill": skill})
        else:
            missing_required.append(skill)
    missing_preferred = [skill for skill in preferred_skills if not match(skill)]
    matched_names = {item["candidate_skill"] for item in matching}
    return {"matching_skills": matching, "missing_required_skills": missing_required, "missing_preferred_skills": missing_preferred, "additional_candidate_skills": [skill for skill in candidate_skills if skill not in matched_names]}
