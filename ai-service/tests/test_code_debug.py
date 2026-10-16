from app.agents.code_debug_agent import CodeDebugAgent


def test_debug_uses_a_distinct_bug_family_for_each_career_module():
    agent = CodeDebugAgent()
    expected = {"matching": "logical", "offers": "runtime", "interviews": "syntax", "profile": "security"}
    for module, bug_type in expected.items():
        result = agent.analyze(module, "python", "def run():\n    return True\n")
        assert result["bug_type"] == bug_type
        assert result["findings"][0]["bug_type"] == bug_type
        assert result["code_was_executed"] is False


def test_matching_detects_python_syntax_and_score_divide_by_zero():
    result = CodeDebugAgent().analyze("matching", "python", "def score(items):\n    score = 100 / len(items)\n    return score\n")
    assert any(item["bug_type"] == "logical" and "zero" in item["message"] for item in result["findings"])


def test_offer_runtime_diagnostic_handles_untrusted_or_missing_fields():
    result = CodeDebugAgent().analyze("offers", "javascript", "const title = offer['title'];")
    assert any(item["bug_type"] == "runtime" for item in result["findings"])


def test_interview_module_reports_python_syntax_location():
    result = CodeDebugAgent().analyze("interviews", "python", "def ask(:\n    pass")
    assert result["findings"][0]["bug_type"] == "syntax"
    assert result["findings"][0]["severity"] == "high"
    assert result["findings"][0]["line"] == 1


def test_profile_module_detects_hardcoded_secrets_and_unsafe_execution():
    result = CodeDebugAgent().analyze("profile", "python", "api_key = 'abcd1234'\neval(user_input)")
    assert {item["bug_type"] for item in result["findings"]} == {"security"}
    assert len(result["findings"]) == 2


def test_debug_request_schema_rejects_unknown_module_and_empty_code():
    import pytest
    from pydantic import ValidationError
    from app.schemas.debug import CodeDebugRequest

    assert CodeDebugRequest(module="matching", language="python", code="x = 1").module == "matching"
    with pytest.raises(ValidationError):
        CodeDebugRequest(module="unknown", language="python", code="x = 1")
    with pytest.raises(ValidationError):
        CodeDebugRequest(module="matching", language="python", code="")


def test_debug_api_handler_returns_module_specific_report():
    from app.api.routes_debug import debug_code
    from app.schemas.debug import CodeDebugRequest

    response = debug_code(CodeDebugRequest(module="profile", language="python", code="password = 'test12345'"))
    assert response["module"] == "profile"
    assert response["findings"][0]["bug_type"] == "security"
