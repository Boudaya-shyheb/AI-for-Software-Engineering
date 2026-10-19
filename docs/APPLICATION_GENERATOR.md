# Application Generator

## Purpose

The Application Generator creates a deterministic application kit from a user's CV and an active job offer. A kit contains an adapted CV, a cover letter, and the matched and missing skills detected from the supplied texts.

## How it works

1. The frontend sends an application-kit request to the Spring API.
2. `ApplicationKitService` authenticates the user, rejects administrator accounts, checks the job offer, selects the pasted CV or the saved profile CV, and calls the AI service.
3. The Spring client sends the generation request to the FastAPI `POST /api/applications/generate` route.
4. `ApplicationAgent` extracts skills, builds the adapted CV from fixed headings and original CV lines, creates a deterministic cover letter, and runs `verify`.
5. Spring checks the returned structure and the `verification.verified` flag. A verified result is saved as an application kit; an unverified or invalid result is rejected.

## No-invention guarantee

- Skills are extracted with the shared `SKILL_ALIASES` and strict boundary matching.
- The adapted CV contains fixed headings and CV lines copied in their original order; it does not rewrite CV lines.
- The cover letter is generated from a fixed template and cites only skills matched between the CV and the offer. When there are no matched skills, it uses a motivation-to-learn paragraph without a skill claim.
- `verify` extracts skills from the generated documents after removing the generated role, greeting, interest sentence, and signature metadata, then compares them with the skills found in the CV.
- Spring returns HTTP 502 and does not save a kit when the verification is false or the AI response is invalid.

## API

| Method | Route | Return codes |
| --- | --- | --- |
| POST | `/api/application-kits` | `201 Created`; `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, or `502 Bad Gateway` on the documented validation, access, offer, AI, or verification errors |
| GET | `/api/application-kits` | `200 OK`; `401 Unauthorized` |
| GET | `/api/application-kits/{id}` | `200 OK`; `401 Unauthorized` or `404 Not Found` |
| PUT | `/api/application-kits/{id}/cover-letter` | `200 OK`; `400 Bad Request`, `401 Unauthorized`, or `404 Not Found` |
| DELETE | `/api/application-kits/{id}` | `204 No Content`; `401 Unauthorized` or `404 Not Found` |
| POST | `/api/applications/generate` | `200 OK`; FastAPI validation or `ValueError` handling returns `422 Unprocessable Entity` |

## Tests

- `ai-service/tests/test_application_generator.py`: 9 tests covering generation, document grounding, verification, strict skill matching, and the FastAPI route.
- `backend/src/test/java/com/aicareeragent/service/ApplicationKitServiceTest.java`: 14 tests covering generation, validation, AI failures, ownership, timestamp behavior, update, and deletion.

## Known limitations

- The no-invention guarantee does not cover a cover letter edited by the user. The frontend signals this state with “Edited by you — not checked against your CV”.
- The AI call takes place inside the `@Transactional` `generate` method.
- `verify` depends on exact text prefixes from the deterministic cover-letter template; this behavior is protected by a test.
- Plural forms are not normalized: “REST APIs” is not recognized as “REST API”.
