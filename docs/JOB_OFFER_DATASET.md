# CareerLens sample job-offer dataset

The repository includes `backend/src/main/resources/data/job-offers.json`: 1,000 fictional offers covering 22 domains. Every record is clearly marked as a fictional demo listing; these are not live vacancies. The first two domains contain 50 offers each and the other 20 domains contain 45 each.

The dataset is generated deterministically from the role, domain, level, employer, and location templates in `scripts/generate-job-offers.mjs`. To regenerate it from the repository root, run:

```sh
node scripts/generate-job-offers.mjs
```

## Loading offers

The Spring Boot seeder loads the bundled JSON when `APP_JOB_OFFERS_SEED_ENABLED=true`. It inserts offers whose title and company are not already present (case-insensitive), so restarting the application does not add duplicates. It does not overwrite user-created offers. The setting defaults to `false` in `application.yml` so production environments do not receive demo data accidentally.

The repository's Docker Compose configuration enables the seeder for local/demo use. If you need to disable sample loading, set `APP_JOB_OFFERS_SEED_ENABLED=false` for the backend service. The frontend catalog supports searching, filtering by domain, and paging through the offers; the admin offer list is also paginated.

## CV matching flow

On the Analyzer page, users can choose **Use my CV** to load their saved profile CV, upload another CV, or paste text. The **Analyze my CV & find matching jobs** button starts the full flow: the backend sends the catalog to the AI service in one request; semantic embeddings are computed in batches; the AI service returns the top 50 offers with compatibility scores and matched/missing skills; and the interface shows the top 12. CareerLens selects the top-ranked offer and generates its report. Users can open another offer or start interview practice with the CV preselected. The matching endpoint is `POST /api/job-offers/matches` and requires the same authenticated session as the rest of the application.
