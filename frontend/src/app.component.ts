import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface AnalysisResult {
  compatibility_score: number;
  models?: { tfidf?: { similarity?: number }; embeddings?: { similarity?: number } };
  skills?: { matching_skills?: Array<{ job_skill: string }>; missing_required_skills?: string[] };
  ai_explanation?: { summary?: string };
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './styles.css',
})
export class AppComponent {
  cvText = '';
  jobDescription = '';
  result: AnalysisResult | null = null;
  loading = false;
  error = '';

  async analyze(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.result = null;
    try {
      const response = await fetch(`${this.apiUrl}/analyses`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cv_text: this.cvText, job_description: this.jobDescription }),
      });
      if (!response.ok) throw new Error('The AI service could not complete this analysis.');
      const payload = await response.json() as { result: AnalysisResult };
      this.result = payload.result;
    } catch (analysisError) {
      this.error = analysisError instanceof Error ? analysisError.message : 'Unexpected analysis error.';
    } finally {
      this.loading = false;
    }
  }

  get apiUrl(): string {
    return 'http://localhost:8080/api';
  }

  score(value: number | undefined): number {
    return Math.round((value ?? 0) * 100);
  }

  compatibilityScore(): number {
    return Math.round((this.result?.compatibility_score ?? 0) * 1000) / 10;
  }
}
