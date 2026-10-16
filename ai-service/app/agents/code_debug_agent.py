import ast
import re


MODULES = {
    "matching": {"label": "CV matching", "bug_type": "logical"},
    "offers": {"label": "Job offers", "bug_type": "runtime"},
    "interviews": {"label": "Interview practice", "bug_type": "syntax"},
    "profile": {"label": "Candidate profile", "bug_type": "security"},
}


class CodeDebugAgent:
    """Local, explainable diagnostics: parsers and curated rules; never executes submitted code."""

    def analyze(self, module: str, language: str, code: str, expected_behavior: str = "") -> dict:
        findings: list[dict] = []
        if language == "python":
            try:
                ast.parse(code)
            except SyntaxError as error:
                findings.append(self._finding("syntax", "high", error.lineno or 1,
                                              f"Python syntax error: {error.msg}.",
                                              "Correct this syntax before running the program."))

        lines = code.splitlines() or [code]
        for number, line in enumerate(lines, 1):
            stripped = line.strip()
            if not stripped or stripped.startswith(("#", "//", "*")):
                continue
            if module == "profile":
                if re.search(r"(?i)(password|api[_-]?key|secret|token)\s*=\s*['\"][^'\"]{4,}['\"]", line):
                    findings.append(self._finding("security", "critical", number,
                        "A credential appears hard-coded in the source.",
                        "Read it from a protected environment variable or secret manager and rotate the exposed value."))
                if re.search(r"(?i)(eval\s*\(|exec\s*\(|dangerouslySetInnerHTML|innerHTML\s*=)", line):
                    findings.append(self._finding("security", "high", number,
                        "This operation can execute or render untrusted input.",
                        "Validate input and use a safe parser or framework escaping instead."))
                if re.search(r"(?i)(SELECT|UPDATE|DELETE|INSERT).*(\+\s*\w+|%s|\{\w+\})", line):
                    findings.append(self._finding("security", "high", number,
                        "Possible SQL built by interpolating user-controlled data.",
                        "Use parameterized queries or the ORM query API."))
            elif module == "matching":
                if re.search(r"(?i)(score|similarity|match).*(/\s*0\b|/\s*len\s*\()", line):
                    findings.append(self._finding("logical", "medium", number,
                        "The matching score may divide by zero for an empty CV, offer, or skill list.",
                        "Handle empty inputs explicitly and define the score for that case."))
                if re.search(r"(?i)(score|similarity|match).*(>|<)\s*100\b", line):
                    findings.append(self._finding("logical", "low", number,
                        "A percentage score is compared against 100; verify whether the internal scale is 0–1 or 0–100.",
                        "Normalize the score once and use the same scale throughout ranking and display."))
            elif module == "offers":
                if re.search(r"(?i)(\[[^\]]*\]|\.get\s*\()", line) and re.search(r"(?i)(offer|job|company|location|salary|description)", line):
                    findings.append(self._finding("runtime", "medium", number,
                        "Offer data may be missing or null at this access point.",
                        "Validate required fields and handle absent optional fields before rendering or processing."))
                if re.search(r"(?i)(parseInt|parseFloat|Integer\.parseInt|Double\.parseDouble)\s*\(", line):
                    findings.append(self._finding("runtime", "medium", number,
                        "Malformed or empty offer values can cause a parsing error.",
                        "Validate the input format and provide a controlled fallback for invalid values."))
            elif module == "interviews":
                if language != "python" and re.search(r"(?<![=!<>])=(?!=)", stripped) and re.search(r"\b(if|while)\s*\([^)]*=[^=]", stripped):
                    findings.append(self._finding("syntax", "medium", number,
                        "An assignment operator appears inside a conditional; this may be an accidental comparison typo.",
                        "Check whether the condition should use the language's equality operator."))
                if re.search(r"(?i)(currentQuestion|current_question|questionIndex|question_index)\s*(\+\+|\+=\s*1)", line):
                    findings.append(self._finding("syntax", "low", number,
                        "Question progression changes the index; check the boundary before reading the next question.",
                        "Guard the final question and keep the index within the question list."))

        if not findings:
            findings.append(self._finding(MODULES[module]["bug_type"], "info", 1,
                "No issue matched the local diagnostic rules in this snippet.",
                "Compare the observed behavior with the expected behavior and add a failing test case."))

        return {
            "module": module,
            "module_label": MODULES[module]["label"],
            "bug_type": MODULES[module]["bug_type"],
            "analysis_method": "local_static_rules",
            "code_was_executed": False,
            "expected_behavior": expected_behavior,
            "findings": findings,
            "summary": f"{len(findings)} diagnostic(s) for {MODULES[module]['label']}; review each suggestion before changing code.",
        }

    @staticmethod
    def _finding(bug_type: str, severity: str, line: int, message: str, recommendation: str) -> dict:
        return {"bug_type": bug_type, "severity": severity, "line": line,
                "message": message, "recommendation": recommendation}
