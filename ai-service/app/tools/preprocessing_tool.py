import re
from dataclasses import dataclass

@dataclass(frozen=True)
class PreprocessedText:
    text: str
    sections: dict[str, str]

def preprocess_cv_text(text: str) -> PreprocessedText:
    if not text or not text.strip():
        raise ValueError("CV text cannot be empty")
    normalized = re.sub(r"\s+", " ", text).strip()
    sections: dict[str, str] = {}
    current = "general"
    for line in text.splitlines():
        value = line.strip()
        if not value:
            continue
        heading = value.lower().rstrip(":")
        if heading in {"skills", "experience", "education", "languages", "certifications", "summary"}:
            current = heading
            sections.setdefault(current, "")
        else:
            sections[current] = f"{sections.get(current, '')} {value}".strip()
    return PreprocessedText(normalized, sections)
