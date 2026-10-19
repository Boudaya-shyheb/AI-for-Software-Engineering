from app.agents.application_agent import ApplicationAgent
from app.agents.career_agent import CareerAgent


def test_generates_application_with_matching_and_missing_skills():
    # Arrange
    agent = ApplicationAgent()
    cv = "Java, Spring Boot, PostgreSQL"
    offer = "Java, Spring Boot, Docker"

    # Act
    result = agent.generate(cv, "Backend Developer", "Acme", offer, "Ada Lovelace")

    # Assert
    assert result["matched_skills"] == ["java", "spring boot"]
    assert result["missing_skills"] == ["docker"]
    assert result["verification"] == {"verified": True, "unsupported_skills": []}


def test_adapted_cv_contains_only_original_cv_lines_besides_fixed_headers():
    # Arrange
    agent = ApplicationAgent()
    cv_lines = ["Java", "Spring Boot", "Built backend APIs", "PostgreSQL"]
    cv = "\n".join(cv_lines)

    # Act
    adapted_cv = agent.generate(
        cv,
        "Backend Developer",
        "Acme",
        "Java, Spring Boot",
        "Ada Lovelace",
    )["adapted_cv"]

    # Assert
    fixed_headers = {
        "Relevant experience",
        "Other information",
    }
    adapted_lines = [
        line for line in adapted_cv.splitlines()
        if line not in fixed_headers
        and not line.startswith("Target role: ")
        and not line.startswith("Key skills for this role: ")
    ]
    assert adapted_lines
    assert all(line in cv_lines for line in adapted_lines)


def test_missing_skill_does_not_appear_in_generated_documents():
    # Arrange
    agent = ApplicationAgent()

    # Act
    result = agent.generate(
        "Java, Spring Boot, PostgreSQL",
        "Backend Developer",
        "Acme",
        "Java, Spring Boot, Docker",
        "Ada Lovelace",
    )

    # Assert
    assert "docker" not in result["adapted_cv"].lower()
    assert "docker" not in result["cover_letter"].lower()


def test_cover_letter_contains_candidate_name_job_title_and_company():
    # Arrange
    agent = ApplicationAgent()

    # Act
    letter = agent.generate(
        "Java developer",
        "Backend Developer",
        "Acme",
        "Java",
        "Ada Lovelace",
    )["cover_letter"]

    # Assert
    assert "Ada Lovelace" in letter
    assert "Backend Developer" in letter
    assert "Acme" in letter


def test_application_without_common_skills_has_no_skill_section_or_skill_claim():
    # Arrange
    agent = ApplicationAgent()

    # Act
    result = agent.generate(
        "Project coordinator with communication experience",
        "Docker and Kubernetes platform engineer",
        "Acme",
        "Docker, Kubernetes",
        "Ada Lovelace",
    )

    # Assert
    assert "Key skills for this role:" not in result["adapted_cv"]
    letter_body = "\n".join(
        line for line in result["cover_letter"].splitlines()
        if not line.startswith("Dear Hiring Manager at ")
        and not line.startswith("I am interested in the ")
        and line not in {"Sincerely,", "Ada Lovelace"}
    )
    assert CareerAgent._extract_skills(letter_body) == []
    assert result["verification"] == {"verified": True, "unsupported_skills": []}


def test_verify_detects_kubernetes_added_to_cover_letter():
    # Arrange
    agent = ApplicationAgent()
    adapted_cv = "Target role: Backend Developer at Acme\nRelevant experience\nJava"
    cover_letter = "I have experience with Kubernetes."

    # Act
    verification = agent.verify("Java developer", adapted_cv, cover_letter)

    # Assert
    assert verification["verified"] is False
    assert verification["unsupported_skills"] == ["kubernetes"]


def test_skill_in_job_title_does_not_fail_verification_when_absent_from_cv():
    # Arrange
    agent = ApplicationAgent()

    # Act
    result = agent.generate(
        "Project coordinator with communication experience",
        "Docker Engineer",
        "Acme",
        "Docker",
        "Ada Lovelace",
    )

    # Assert
    assert result["matched_skills"] == []
    assert result["verification"] == {"verified": True, "unsupported_skills": []}


def test_javascript_does_not_match_java_skill():
    # Arrange
    agent = ApplicationAgent()

    # Act
    result = agent.generate(
        "Frontend developer with JavaScript",
        "Backend Java developer",
        "Acme",
        "Java",
        "Ada Lovelace",
    )

    # Assert
    assert "java" not in result["matched_skills"]


def test_application_route_rejects_empty_cv_and_accepts_valid_request():
    # Arrange
    from fastapi.testclient import TestClient
    from app.main import app

    client = TestClient(app)
    payload = {
        "cv_text": "Java developer",
        "job_title": "Backend Developer",
        "company": "Acme",
        "job_description": "Java",
        "candidate_name": "Ada Lovelace",
    }

    # Act
    empty_cv_response = client.post(
        "/api/applications/generate",
        json={**payload, "cv_text": ""},
    )
    valid_response = client.post("/api/applications/generate", json=payload)

    # Assert
    assert empty_cv_response.status_code == 422
    assert valid_response.status_code == 200
    assert set(valid_response.json()) == {
        "adapted_cv",
        "cover_letter",
        "matched_skills",
        "missing_skills",
        "verification",
    }
