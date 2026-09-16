# AI Career Agent

Plateforme d'analyse de CV et de matching d'offres, construite autour d'un Agent IA qui orchestre des outils spécialisés.

## Démarrage

```bash
cp .env.example .env
docker compose up --build
```

Services :

- Frontend React : http://localhost:5173
- API Spring Boot : http://localhost:8080/health
- Service IA FastAPI : http://localhost:8000/docs
- PostgreSQL : localhost:5432

## Analyse IA

Le endpoint `POST /api/analyses` du service IA accepte `cv_text` et `job_description`. Il exécute l'extraction structurée, TF-IDF, Sentence Transformer (avec fallback local explicite si le modèle n'est pas disponible), matching et scoring déterministe.

```json
{
	"cv_text": "Skills\nJava Spring Boot",
	"job_description": "Java Spring Boot developer"
}
```

Le score retourné est un **AI Compatibility Score**, et ne représente pas une probabilité d'embauche.