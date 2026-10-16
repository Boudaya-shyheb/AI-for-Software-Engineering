from app.agents.interview_agent import InterviewAgent


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


def test_complete_practice_cycle_generates_evaluates_and_summarizes():
    agent = InterviewAgent()
    cv = "Junior backend developer with Java, Spring Boot, PostgreSQL, and REST API experience."
    offer = "Junior Java engineer. Required: Spring Boot, PostgreSQL, Docker, REST API, teamwork."

    practice = agent.generate(cv, offer, 8)

    assert len(practice["questions"]) == 8
    assert "java" in practice["profile"]["matching_skills"]
    assert "docker" in practice["profile"]["skill_gaps"]
    question = next(item for item in practice["questions"] if item["type"] == "technical")
    answer = "For example, I used Java and Spring Boot to build a REST API with PostgreSQL. I tested each endpoint and shared the result with my team."
    evaluation = agent.evaluate(question["question"], answer, cv, offer, question["type"])
    summary = agent.summarize([evaluation])

    assert 0 <= evaluation["score"] <= 100
    assert evaluation["strengths"]
    assert evaluation["feedback"].startswith("Your answer")
    assert summary["score"] == evaluation["score"]
    assert summary["strengths"]
    assert summary["recommendations"]


def test_interview_api_rejects_empty_cv():
    from fastapi.testclient import TestClient
    from app.main import app

    response = TestClient(app).post("/api/interviews/generate", json={"cv_text": "", "job_description": "Python"})
    assert response.status_code == 422
