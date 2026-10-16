import re
from collections import Counter


class InterviewAgent:
    """Question and feedback generation grounded in the supplied CV and role."""

    STOPWORDS = {"with", "that", "this", "from", "your", "have", "will", "their", "about", "into", "years", "experience", "required", "skills", "the", "and", "for", "les", "des", "une", "dans", "pour", "avec", "sur", "est", "sont", "vous", "nous"}

    @classmethod
    def terms(cls, text: str) -> list[str]:
        words = re.findall(r"[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9+#.-]{2,}", text)
        counts = Counter(word.strip(".-").lower() for word in words)
        return [word for word, _ in counts.most_common() if word not in cls.STOPWORDS][:20]

    def generate(self, cv_text: str, job_description: str, question_count: int) -> dict:
        cv_terms = set(self.terms(cv_text))
        role_terms = self.terms(job_description)
        if not role_terms:
            role_terms = ["teamwork"]
        matched = [term for term in role_terms if term in cv_terms]
        gaps = [term for term in role_terms if term not in cv_terms]
        prompts = []
        for term in matched[:max(1, question_count // 3)]:
            prompts.append({"type": "technical", "question": f"How have you used {term}? Please give one example."})
        for term in gaps[:max(1, question_count // 3)]:
            prompts.append({"type": "technical", "question": f"This job needs {term}. How would you get started with it?"})
        prompts.extend([
            {"type": "behavioral", "question": "Tell me about a problem you solved at work or school. What did you do?"},
            {"type": "behavioral", "question": "Tell me about a time you learned something new. How did you learn it?"},
            {"type": "behavioral", "question": "When you have many tasks, how do you choose what to do first?"},
            {"type": "motivation", "question": "What interests you most about this job?"},
        ])
        selected = prompts[:question_count]
        while len(selected) < question_count:
            term = role_terms[len(selected) % len(role_terms)]
            selected.insert(-1, {"type": "technical", "question": f"How would you use {term} in this job? What first step would you take?"})
        return {"questions": selected, "profile": {"matching_skills": matched[:8], "skill_gaps": gaps[:8], "summary": f"Questions are grounded in {len(matched)} role terms also found in the CV and target skills from the offer."}}

    def evaluate(self, question: str, answer: str, cv_text: str, job_description: str, question_type: str) -> dict:
        answer_terms = set(self.terms(answer))
        context = set(self.terms(cv_text + " " + job_description))
        relevant = answer_terms & context
        sentences = [part.strip() for part in re.split(r"[.!?]+", answer) if part.strip()]
        has_example = any(marker in answer.lower() for marker in ("for example", "for instance", "because", "result", "outcome", "i did", "we did"))
        score = min(95, 35 + min(30, len(relevant) * 7) + min(20, len(sentences) * 4) + (15 if has_example else 0))
        strengths = []
        if relevant:
            strengths.append("Your answer mentions skills related to your CV or this job: " + ", ".join(sorted(relevant)[:5]) + ".")
        if len(sentences) >= 2:
            strengths.append("You explained more than one step or idea.")
        if has_example:
            strengths.append("You gave an example or result.")
        if not strengths:
            strengths.append("You answered the question directly.")
        improvements = []
        if len(answer.split()) < 35:
            improvements.append("Add a little more detail: what was the situation, and what did you do?")
        if not has_example:
            improvements.append("Give one real example and say what happened.")
        if not relevant:
            improvements.append("Link your answer to a skill this job needs.")
        return {"score": score, "feedback": "Your answer was checked for relevance, clear detail, and examples.", "strengths": strengths, "improvements": improvements}

    @staticmethod
    def summarize(evaluations: list[dict]) -> dict:
        scores = [int(item.get("score", 0)) for item in evaluations]
        strengths = list(dict.fromkeys(text for item in evaluations for text in item.get("strengths", [])))[:5]
        improvements = list(dict.fromkeys(text for item in evaluations for text in item.get("improvements", [])))[:5]
        return {"score": round(sum(scores) / len(scores)) if scores else 0, "strengths": strengths, "weaknesses": improvements, "recommendations": improvements[:3] or ["Keep using clear examples and explain what changed because of your work."]}
