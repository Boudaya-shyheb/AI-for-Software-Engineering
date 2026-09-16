from pathlib import Path

def extract_cv_text(file_path: str) -> dict:
    path = Path(file_path)
    if not path.exists() or not path.is_file():
        raise FileNotFoundError("CV file does not exist")
    if path.suffix.lower() == '.pdf':
        from pypdf import PdfReader
        pages = PdfReader(str(path)).pages
        text = '\n'.join(page.extract_text() or '' for page in pages)
        return {'text': text, 'pages': len(pages), 'characters': len(text)}
    if path.suffix.lower() == '.docx':
        from docx import Document
        text = '\n'.join(paragraph.text for paragraph in Document(str(path)).paragraphs)
        return {'text': text, 'pages': 1, 'characters': len(text)}
    raise ValueError('Unsupported CV format; expected PDF or DOCX')
