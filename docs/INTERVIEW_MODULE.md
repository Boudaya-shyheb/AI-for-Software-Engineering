# Simulateur d’entretien personnalisé

## Parcours utilisateur

1. Depuis **Interview practice**, l’utilisateur sélectionne une offre publiée et importe un CV PDF, DOCX ou TXT (ou colle son texte).
2. Spring Boot vérifie l’offre et le compte connecté, puis transmet CV + description à FastAPI.
3. Le service IA compare les compétences extraites du CV et de l’offre, puis prépare huit questions techniques, comportementales et de motivation. Le profil de compétences est visible dans la simulation et conservé dans l’historique.
4. Les questions sont formulées en anglais simple, avec une seule demande à la fois. L’utilisateur répond à chaque question et reçoit une note sur 100, des points forts et des pistes d’amélioration en anglais.
5. Après la dernière réponse, un bilan en anglais présente le score moyen, les points forts, les axes à travailler et des recommandations.
6. La session reste disponible dans l’historique du compte et peut être rouverte pour consulter le bilan ou reprendre une simulation en cours.

## API

Toutes les routes Spring nécessitent le jeton JWT existant.

| Méthode | Route | Usage |
| --- | --- | --- |
| `POST` | `/api/interviews` | Créer une simulation (`offerId`, `cvText`) |
| `GET` | `/api/interviews` | Historique du compte connecté |
| `GET` | `/api/interviews/{id}` | Détail d’une session appartenant au compte |
| `POST` | `/api/interviews/{id}/answers` | Évaluer et enregistrer une réponse |
| `GET` | `/api/admin/interviews` | Résultats de toutes les simulations, réservé aux administrateurs |

FastAPI expose en interne `/api/interviews/generate`, `/evaluate` et `/summary`. Spring orchestre ces appels comme pour l’analyse de CV existante. Les réponses invalides produisent une erreur 422, une offre absente ou une session qui n’appartient pas à l’utilisateur une erreur 404, et une session terminée ne peut plus recevoir de réponse (409).

## Stockage et données

Les sessions sont enregistrées dans `interview_sessions` avec une relation vers `users`. Les questions, réponses, évaluations et bilan sont sérialisés en texte JSON dans PostgreSQL pour éviter d’ajouter une dépendance. Le CV et la description de l’offre sont conservés pour pouvoir terminer la session et relire les évaluations. Chaque requête de lecture est filtrée par l’identifiant du propriétaire.

## Évaluation IA

Le service actuel ne configure aucun modèle génératif externe. Ce module réutilise donc l’agent déterministe FastAPI et ses capacités locales : extraction de termes CV/offre, questions basées sur les compétences correspondantes ou manquantes, puis évaluation heuristique de la pertinence, du niveau de détail et des exemples concrets. Aucun secret ni fournisseur externe n’est nécessaire. Les notes sont des retours d’entraînement, pas une mesure objective de performance professionnelle.

## Vérification

Tests ciblés dans `ai-service/tests/test_interviews.py` couvrent la personnalisation des questions, l’évaluation des réponses et la validation d’entrée. `backend/src/test/java/com/aicareeragent/service/InterviewServiceTest.java` couvre la création, l’évaluation, la fin de session et l’isolation par propriétaire.

La vue admin liste le candidat, l’offre, l’état d’avancement, le score global et le feedback de chaque réponse. Elle ne renvoie ni le texte du CV ni les réponses brutes des utilisateurs.

Vérifications exécutées pendant l’intégration : 9 tests pytest réussis, 6 tests JUnit couvrant le parcours utilisateur et la vue admin, compilation de production Angular réussie et build Maven de production réussi. Une stack Docker temporaire a également validé la création de l’offre, la génération des huit questions, les huit évaluations, le bilan et l’historique de bout en bout.
