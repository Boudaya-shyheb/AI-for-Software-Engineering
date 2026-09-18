import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
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
  readonly result = signal<AnalysisResult | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  async analyze(): Promise<void> {
    if (this.loading()) return;

    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), 120_000);
    try {
      const response = await fetch(`${this.apiUrl}/analyses`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cv_text: this.cvText, job_description: this.jobDescription }),
        signal: controller.signal,
      });
      if (!response.ok) {
        const errorPayload = await response.json().catch(() => null) as { detail?: string } | null;
        throw new Error(errorPayload?.detail ?? 'The AI service could not complete this analysis.');
      }
      const payload = await response.json() as { result: AnalysisResult };
      if (!payload.result) throw new Error('The AI service returned an invalid response.');
      this.result.set(payload.result);
    } catch (analysisError) {
      this.error.set(analysisError instanceof DOMException && analysisError.name === 'AbortError'
        ? 'The analysis took too long. Please try again.'
        : analysisError instanceof Error ? analysisError.message : 'Unexpected analysis error.');
    } finally {
      window.clearTimeout(timeoutId);
      this.loading.set(false);
    }
  }

  get apiUrl(): string {
    return '/api';
  }

  score(value: number | undefined): number {
    return Math.round((value ?? 0) * 100);
  }

  compatibilityScore(): number {
    return Math.round((this.result()?.compatibility_score ?? 0) * 1000) / 10;
  }
}
