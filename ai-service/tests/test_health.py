from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    response = client.get('/health')
    assert response.status_code == 200
    assert response.json()['status'] == 'ok'

def test_analysis_validates_empty_input():
    response = client.post('/api/analyses', json={'cv_text': '', 'job_description': 'Java'})
    assert response.status_code == 422
