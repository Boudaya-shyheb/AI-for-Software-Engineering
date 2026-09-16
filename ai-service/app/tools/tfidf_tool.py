from time import perf_counter
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

def calculate_tfidf_similarity(cv_text: str, job_description: str) -> dict:
    if not cv_text.strip() or not job_description.strip():
        raise ValueError("Both texts are required")
    started = perf_counter()
    vectors = TfidfVectorizer(stop_words="english").fit_transform([cv_text, job_description])
    similarity = float(cosine_similarity(vectors[0:1], vectors[1:2])[0][0])
    return {"method": "TF-IDF", "similarity": round(similarity, 6), "execution_time_ms": round((perf_counter() - started) * 1000, 2)}
