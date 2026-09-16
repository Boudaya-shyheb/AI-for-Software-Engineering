from app.agents.career_agent import CareerAgent
from app.tools.scoring_tool import ScoreInput, calculate_compatibility_score
from app.tools.skill_matching_tool import compare_skills

def test_score_is_deterministic():
    result = calculate_compatibility_score(ScoreInput(skills_score=.9, semantic_score=.84, experience_score=.75, education_score=1))
    assert result['final_score'] == .851

def test_skill_matching_accepts_close_terms():
    result = compare_skills(['Spring Boot'], ['Spring Framework'], [])
    assert result['matching_skills']

def test_agent_returns_complete_report():
    result = CareerAgent().analyze('Skills\nPython Java', 'Python developer')
    assert result['status'] == 'COMPLETED'
    assert 0 <= result['compatibility_score'] <= 1
