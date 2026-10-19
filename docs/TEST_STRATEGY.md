# CareerLens test strategy

These tests target application behavior and module workflows. They do not stop at checking whether a form accepts typed values.

| Module | Test types | Behaviors covered |
| --- | --- | --- |
| CV matching | AI unit/workflow tests, FastAPI route contract tests, Spring controller workflow tests | Offer ranking, score ordering, matching-skill explanations and gaps, empty catalog behavior, published-offer filtering, passing the ranked catalog to the AI service, and saving a completed score for the authenticated user. |
| Job offers | Controller workflow tests | Public catalog excludes archived offers; admin archive changes availability without deleting the offer; linked offers cannot be deleted. |
| Candidate profile | Controller workflow tests | Profile reads use the authenticated account; profile updates persist the saved CV so a later matching session can reuse it. |
| Interview practice | AI workflow tests, service state-transition tests, admin privacy/reporting tests | Questions use CV and role context, answer evaluation and final summary work together, completed sessions reject more answers, users cannot read another user's session, and admin results omit CV text and raw answers. |
| Application Generator | AI generator tests, service unit tests | Deterministic document generation, no-invention verification, AI failure handling, ownership checks, timestamp behavior, cover-letter updates, and kit deletion. |

## Run

From the repository root on Windows:

```powershell
.\ai-service\.venv\Scripts\python.exe -m pytest -c .\ai-service\pytest.ini .\ai-service
```

The backend tests use the portable JDK 21 and Maven installed under the ignored `.tools/` directory:

```powershell
.\scripts\test-backend.ps1
```

The Python environment in `ai-service/.venv/` contains the test and runtime dependencies needed for the fallback TF-IDF path; large optional PyTorch/Sentence Transformers packages are not installed. The Python matching tests replace the embedding call with a deterministic function, so they do not download or initialize a language model. The Java controller tests use Mockito for repository and AI boundaries; they do not need Docker or a running PostgreSQL instance. No frontend test runner has been configured in this repository yet, so the Angular screens are not covered by browser end-to-end automation in this test set.
