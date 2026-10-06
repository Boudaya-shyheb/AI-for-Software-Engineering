from app.agents.interview_agent import InterviewAgent
from fastapi.testclient import TestClient
from app.main import app


def test_generated_questions_use_profile_and_role_terms():
    result = InterviewAgent().generate(
        "Python developer with Django, PostgreSQL, and REST API experience",
        "We need Django and cloud deployment skills with PostgreSQL",
        8,
    )
    assert len(result["questions"]) == 8
    assert any("django" in item["question"].lower() for item in result["questions"])
    assert {"django", "postgresql"}.issubset(result["profile"]["matching_skills"])
    assert "cloud" in result["profile"]["skill_gaps"]
    assert any(item["type"] == "behavioral" for item in result["questions"])
    assert all("concrete situation" not in item["question"].lower() for item in result["questions"])


def test_evaluation_scores_concrete_relevant_answer():
    agent = InterviewAgent()
    result = agent.evaluate(
        "Describe your Python work", "For example, I used Python and Django to ship a result.",
        "Python Django developer", "Python Django role", "technical",
    )
    assert result["score"] > 50
    assert result["strengths"]
    assert result["feedback"].startswith("Your answer")
    assert all(text.isascii() for text in result["strengths"] + result["improvements"])


def test_summary_feedback_is_in_english():
    result = InterviewAgent.summarize([{"score": 80, "strengths": [], "improvements": []}])
    assert result["recommendations"][0].startswith("Keep using")


def test_interview_api_rejects_empty_cv():
    response = TestClient(app).post("/api/interviews/generate", json={"cv_text": "", "job_description": "Python"})
    assert response.status_code == 422
