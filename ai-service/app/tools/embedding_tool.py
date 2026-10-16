from time import perf_counter
import os

_model = None

def calculate_embedding_similarity(cv_text: str, job_description: str) -> dict:
    return calculate_embedding_similarities(cv_text, [job_description])[0]

def calculate_embedding_similarities(cv_text: str, job_descriptions: list[str]) -> list[dict]:
    if not cv_text.strip() or any(not description.strip() for description in job_descriptions):
        raise ValueError("CV text and job descriptions are required")
    if not job_descriptions:
        return []
    started = perf_counter()
    model_name = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    global _model
    try:
        from sentence_transformers import SentenceTransformer
        if _model is None:
            _model = SentenceTransformer(model_name)
        cv_embedding = _model.encode([cv_text], normalize_embeddings=True)[0]
        similarities = []
        for offset in range(0, len(job_descriptions), 64):
            batch = _model.encode(job_descriptions[offset:offset + 64], normalize_embeddings=True)
            similarities.extend(float(cv_embedding @ embedding) for embedding in batch)
        method = "Sentence Transformer"
    except (ImportError, OSError):
        from sklearn.feature_extraction.text import TfidfVectorizer
        from sklearn.metrics.pairwise import cosine_similarity
        vectors = TfidfVectorizer(stop_words="english").fit_transform([cv_text, *job_descriptions])
        similarities = cosine_similarity(vectors[1:], vectors[0:1]).ravel().tolist()
        method = "Sentence Transformer (fallback: TF-IDF)"
    elapsed = round((perf_counter() - started) * 1000, 2)
    return [
        {"method": method, "model_name": model_name, "similarity": round(max(0.0, min(1.0, similarity)), 6), "execution_time_ms": elapsed}
        for similarity in similarities
    ]
