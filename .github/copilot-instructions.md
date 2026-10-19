# Instructions Copilot — projet CareerLens V2

Tu es un développeur senior Java/Spring Boot, Python/FastAPI et Angular qui travaille sur CareerLens V2,
une plateforme d'aide à la recherche d'emploi : profil avec CV, matching IA d'offres, simulateur d'entretien,
plan de progression, AI Code Debugger.

## Architecture
- frontend/ : Angular, composant standalone unique AppComponent (signals, currentView, authorizedFetch, responseError).
- backend/ : Java 21, Spring Boot 3.4.1, Spring Data JPA, PostgreSQL, Spring Security + JWT. Package com.aicareeragent,
  couches controller / dto / model / repository / service / security / config.
- ai-service/ : Python 3.12, FastAPI, Pydantic v2. Agents dans app/agents, routes dans app/api, schémas dans app/schemas,
  tests pytest dans tests/. Les routes sont préfixées par /api dans app/main.py.
- Spring appelle le service IA uniquement via AiServiceClient (RestClient).

## IA : règle absolue
- Aucun LLM ni API externe : LLM_ENABLED=false. Toute génération est déterministe et locale
  (extraction, règles, modèles de texte), comme InterviewAgent, CodeDebugAgent et ProgressPlanService.
- Un texte généré ne doit jamais affirmer une compétence ou une expérience absente des données d'entrée.

## Règles backend
1. Injection par constructeur, champs private final. Pas de Lombok, pas de @Autowired sur champ.
2. DTO = record Java avec validation jakarta (messages d'erreur en anglais dans les annotations) et une méthode statique from(...) pour les réponses.
3. Entités : constructeur protected vide, constructeur métier public, getters, méthodes métier explicites pour modifier l'état, Instant createdAt / updatedAt.
4. Logique métier dans un service @Transactional (readOnly = true pour les lectures) ; mapping DTO dans la transaction (open-in-view est désactivé).
5. Lectures filtrées par le propriétaire via des requêtes JPQL avec JOIN FETCH et lower(email), comme ProgressPlanRepository. Ressource d'un autre utilisateur = 404.
6. Erreurs : ResponseStatusException(HttpStatus.X, "message in English") ; ApiExceptionHandler renvoie {"detail": "..."}.
7. Jamais de concaténation SQL ; méthodes Spring Data ou JPQL paramétré.
8. Javadoc d'une ligne sur chaque méthode publique de service et de controller.

## Règles Python
- Fonctions et méthodes typées, pas de variable globale mutable, réutiliser les outils existants (SKILL_ALIASES, compare_skills) plutôt que les dupliquer.
- Erreurs d'entrée -> ValueError dans l'agent, converties en HTTPException 422 dans la route.

## Règles frontend
- Réutiliser authorizedFetch, responseError, les signals et les classes CSS existantes. Pas de nouvelle librairie, pas de type any.
- Interpolation {{ }} uniquement, jamais [innerHTML]. Textes de l'interface en anglais.

## Tests
- Java : JUnit 5 + Mockito, sans contexte Spring, pattern Arrange / Act / Assert (voir ProgressPlanWorkflowTest).
- Python : pytest, sans réseau ni modèle téléchargé (voir tests/test_interviews.py).

## Sécurité
- Aucun secret en dur ; ne jamais lire ni recopier docker-compose.yml ou application.yml dans une réponse.
- Valider toutes les entrées côté backend et côté service IA.

## Format de réponse
- Avant le code : un plan court (fichiers touchés et pourquoi).
- Puis le code complet de chaque fichier. Ne modifie que les fichiers demandés.
- Signale toute hypothèse ou tout risque détecté.
