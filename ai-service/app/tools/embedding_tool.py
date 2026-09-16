from time import perf_counter
import os

_model = None

def calculate_embedding_similarity(cv_text: str, job_description: str) -> dict:
    if not cv_text.strip() or not job_description.strip():
        raise ValueError("Both texts are required")
    started = perf_counter()
    model_name = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    global _model
    try:
        from sentence_transformers import SentenceTransformer
        if _model is None:
            _model = SentenceTransformer(model_name)
        embeddings = _model.encode([cv_text, job_description], normalize_embeddings=True)
        similarity = float(embeddings[0] @ embeddings[1])
        method = "Sentence Transformer"
    except (ImportError, OSError):
        from sklearn.feature_extraction.text import TfidfVectorizer
        from sklearn.metrics.pairwise import cosine_similarity
        vectors = TfidfVectorizer(stop_words="english").fit_transform([cv_text, job_description])
        similarity = float(cosine_similarity(vectors[0:1], vectors[1:2])[0][0])
        method = "Sentence Transformer (fallback: TF-IDF)"
    return {"method": method, "model_name": model_name, "similarity": round(max(0.0, min(1.0, similarity)), 6), "execution_time_ms": round((perf_counter() - started) * 1000, 2)}
