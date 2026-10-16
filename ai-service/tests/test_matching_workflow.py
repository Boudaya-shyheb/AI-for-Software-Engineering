from fastapi.testclient import TestClient

from app.agents import career_agent
from app.main import app


def test_matching_workflow_ranks_offers_and_explains_skill_gaps(monkeypatch):
    """Exercise match scoring, ordering, and explanations without loading a model."""
    monkeypatch.setattr(
        career_agent,
        "calculate_embedding_similarities",
        lambda _cv, descriptions: [
            {"similarity": 0.99 if "frontend" in description.lower() else 0.2}
            for description in descriptions
        ],
    )
    agent = career_agent.CareerAgent()

    result = agent.match_jobs(
        "Frontend engineer with Angular, TypeScript and JavaScript experience",
        [
            {"id": 11, "title": "Data Engineer", "company": "DataCo", "description": "Core skills: Python, Spark, SQL"},
            {"id": 12, "title": "Frontend Engineer", "company": "WebCo", "description": "Frontend role. Core skills: Angular, TypeScript, JavaScript"},
        ],
    )

    assert result["total_offers"] == 2
    assert [item["offer"]["id"] for item in result["matches"]] == [12, 11]
    assert {item["job_skill"].lower() for item in result["matches"][0]["matching_skills"]} >= {"angular", "typescript"}
    assert "python" in {skill.lower() for skill in result["matches"][1]["missing_required_skills"]}


def test_matching_api_handles_empty_catalog_without_calling_a_model():
    response = TestClient(app).post(
        "/api/job-matches",
        json={"cv_text": "Experienced backend engineer with Java", "offers": []},
    )

    assert response.status_code == 200
    assert response.json() == {"matches": [], "total_offers": 0}


def test_matching_api_rejects_blank_cv_as_a_workflow_error():
    response = TestClient(app).post(
        "/api/job-matches",
        json={"cv_text": " ", "offers": []},
    )

    assert response.status_code == 422
