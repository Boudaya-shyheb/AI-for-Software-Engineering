# AI-assisted code diagnostics

CareerLens now provides an authenticated **AI Code Debugger** workspace. Choose a product area, a supported language, paste a snippet, and optionally describe expected behavior. The backend forwards the request to the existing FastAPI AI service and returns findings with severity, line number, explanation, and a suggested next step.

## Module-specific bug focus

| CareerLens area | Primary bug family | Example checks |
| --- | --- | --- |
| CV matching | Logical | Empty-input division and inconsistent score scales |
| Job offers | Runtime | Missing offer fields and unsafe numeric parsing |
| Interview practice | Syntax / control flow | Python parser errors, suspicious conditional assignment, question index bounds |
| Candidate profile | Security | Hard-coded credentials, unsafe evaluation/rendering, SQL interpolation |

The module selection sets the main bug family shown in the result. A Python parser error can also be reported for any selected module because syntax must be valid before further analysis.

## API

`POST /api/code-debug` requires the same bearer token as other application endpoints. Request fields:

```json
{
  "module": "matching",
  "language": "python",
  "code": "def score(items):\\n    return 100 / len(items)",
  "expected_behavior": "Return a safe score for an empty list"
}
```

Accepted modules: `matching`, `offers`, `interviews`, `profile`. Accepted languages: `python`, `java`, `javascript`, `typescript`. Code is limited to 20,000 characters; expected behavior to 2,000.

## Scope and safety

Diagnostics currently use local Python syntax parsing and a small, explainable set of static rules. Submitted code is **not executed**, persisted, or sent to a third-party model. The endpoint does not generate or apply patches. It is a first implementation of the detection/localization/recommendation loop, not a semantic LLM review; a clean report does not prove that code is correct. Review suggestions and validate changes with the relevant tests. Extending this to semantic AI recommendations requires selecting and configuring a model provider or deploying a local code model.

## Verification

Python API and rule tests are in `ai-service/tests/test_code_debug.py`. Backend request validation is enforced by `CodeDebugRequest`; authentication is inherited from the application's authenticated `/api/**` routes. The frontend calls this endpoint with its existing authorized fetch helper.
