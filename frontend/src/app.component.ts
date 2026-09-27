import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface AnalysisResult {
  compatibility_score: number;
  models?: { tfidf?: { similarity?: number }; embeddings?: { similarity?: number } };
  skills?: { matching_skills?: Array<{ job_skill: string }>; missing_required_skills?: string[] };
  ai_explanation?: { summary?: string };
}

interface AuthUser {
  id: number;
  fullName: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

interface AuthPayload { token: string; user: AuthUser; }
interface AdminStats { users: number; analyses: number; jobOffers: number; }

interface JobOffer {
  id: number;
  title: string;
  company: string;
  location: string;
  employmentType: string;
  description: string;
  createdAt: string;
  updatedAt: string;
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
  fullName = '';
  email = '';
  password = '';

  readonly result = signal<AnalysisResult | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly token = signal(localStorage.getItem('careerlens_token') ?? '');
  readonly user = signal<AuthUser | null>(this.readStoredUser());
  readonly authMode = signal<'login' | 'register'>('login');
  readonly authLoading = signal(false);
  readonly authError = signal('');
  readonly currentView = signal<'analyzer' | 'jobs' | 'admin'>('analyzer');
  readonly uploadingCv = signal(false);
  readonly uploadingJob = signal(false);
  readonly cvFilename = signal('');
  readonly jobFilename = signal('');
  readonly adminStats = signal<AdminStats | null>(null);
  readonly adminUsers = signal<AuthUser[]>([]);
  readonly adminLoading = signal(false);
  readonly jobOffers = signal<JobOffer[]>([]);
  readonly offersLoading = signal(false);
  readonly selectedOffer = signal<JobOffer | null>(null);
  readonly adminOffers = signal<JobOffer[]>([]);
  readonly editingOfferId = signal<number | null>(null);
  readonly offerSaving = signal(false);
  readonly offerError = signal('');
  offerTitle = '';
  offerCompany = '';
  offerLocation = '';
  offerEmploymentType = 'CDI';
  offerDescription = '';
  readonly visibleResult = computed(() => this.currentView() === 'analyzer' || this.currentView() === 'jobs' ? this.result() : null);

  async submitAuth(): Promise<void> {
    if (this.authLoading()) return;
    this.authLoading.set(true);
    this.authError.set('');
    const endpoint = this.authMode() === 'login' ? 'login' : 'register';
    const body = this.authMode() === 'login'
      ? { email: this.email, password: this.password }
      : { fullName: this.fullName, email: this.email, password: this.password };
    try {
      const response = await fetch(`${this.apiUrl}/auth/${endpoint}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Authentication failed'));
      const payload = await response.json() as AuthPayload;
      localStorage.setItem('careerlens_token', payload.token);
      localStorage.setItem('careerlens_user', JSON.stringify(payload.user));
      this.token.set(payload.token);
      this.user.set(payload.user);
      this.password = '';
    } catch (authError) {
      this.authError.set(authError instanceof Error ? authError.message : 'Authentication failed');
    } finally {
      this.authLoading.set(false);
    }
  }

  switchAuthMode(mode: 'login' | 'register'): void {
    this.authMode.set(mode);
    this.authError.set('');
  }

  logout(): void {
    localStorage.removeItem('careerlens_token');
    localStorage.removeItem('careerlens_user');
    this.token.set('');
    this.user.set(null);
    this.result.set(null);
    this.currentView.set('analyzer');
  }

  async analyze(): Promise<void> {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), 120_000);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/analyses`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cv_text: this.cvText, job_description: this.jobDescription }),
        signal: controller.signal,
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'The AI service could not complete this analysis.'));
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

  async uploadDocument(event: Event, target: 'cv' | 'job'): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const uploading = target === 'cv' ? this.uploadingCv : this.uploadingJob;
    uploading.set(true);
    this.error.set('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await this.authorizedFetch(`${this.apiUrl}/documents/extract`, { method: 'POST', body: formData });
      if (!response.ok) throw new Error(await this.responseError(response, 'The document could not be imported.'));
      const payload = await response.json() as { text: string; filename: string };
      if (target === 'cv') {
        this.cvText = payload.text;
        this.cvFilename.set(payload.filename);
      } else {
        this.jobDescription = payload.text;
        this.jobFilename.set(payload.filename);
      }
    } catch (uploadError) {
      this.error.set(uploadError instanceof Error ? uploadError.message : 'Document upload failed.');
    } finally {
      uploading.set(false);
      input.value = '';
    }
  }

  async openAdmin(): Promise<void> {
    this.currentView.set('admin');
    this.adminLoading.set(true);
    this.error.set('');
    try {
      const [statsResponse, usersResponse, offersResponse] = await Promise.all([
        this.authorizedFetch(`${this.apiUrl}/admin/stats`),
        this.authorizedFetch(`${this.apiUrl}/admin/users`),
        this.authorizedFetch(`${this.apiUrl}/admin/job-offers`),
      ]);
      if (!statsResponse.ok || !usersResponse.ok || !offersResponse.ok) throw new Error('Unable to load the admin dashboard.');
      this.adminStats.set(await statsResponse.json() as AdminStats);
      this.adminUsers.set(await usersResponse.json() as AuthUser[]);
      this.adminOffers.set(await offersResponse.json() as JobOffer[]);
    } catch (adminError) {
      this.error.set(adminError instanceof Error ? adminError.message : 'Admin dashboard failed to load.');
    } finally {
      this.adminLoading.set(false);
    }
  }

  async openJobs(): Promise<void> {
    this.currentView.set('jobs');
    this.selectedOffer.set(null);
    this.result.set(null);
    this.error.set('');
    await this.loadJobOffers();
  }

  async loadJobOffers(): Promise<void> {
    this.offersLoading.set(true);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/job-offers`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load job offers.'));
      this.jobOffers.set(await response.json() as JobOffer[]);
    } catch (offersError) {
      this.error.set(offersError instanceof Error ? offersError.message : 'Unable to load job offers.');
    } finally {
      this.offersLoading.set(false);
    }
  }

  selectOffer(offer: JobOffer): void {
    this.selectedOffer.set(offer);
    this.jobDescription = offer.description;
    this.result.set(null);
    this.error.set('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeOffer(): void {
    this.selectedOffer.set(null);
    this.result.set(null);
    this.cvText = '';
    this.cvFilename.set('');
  }

  async matchSelectedOffer(): Promise<void> {
    const offer = this.selectedOffer();
    if (!offer) return;
    this.jobDescription = offer.description;
    await this.analyze();
  }

  editOffer(offer: JobOffer): void {
    this.editingOfferId.set(offer.id);
    this.offerTitle = offer.title;
    this.offerCompany = offer.company;
    this.offerLocation = offer.location;
    this.offerEmploymentType = offer.employmentType;
    this.offerDescription = offer.description;
    this.offerError.set('');
  }

  resetOfferForm(): void {
    this.editingOfferId.set(null);
    this.offerTitle = '';
    this.offerCompany = '';
    this.offerLocation = '';
    this.offerEmploymentType = 'CDI';
    this.offerDescription = '';
    this.offerError.set('');
  }

  async saveOffer(): Promise<void> {
    if (this.offerSaving()) return;
    this.offerSaving.set(true);
    this.offerError.set('');
    const id = this.editingOfferId();
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/admin/job-offers${id ? `/${id}` : ''}`, {
        method: id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: this.offerTitle,
          company: this.offerCompany,
          location: this.offerLocation,
          employmentType: this.offerEmploymentType,
          description: this.offerDescription,
        }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to save this job offer.'));
      this.resetOfferForm();
      await this.refreshAdminOffers();
    } catch (saveError) {
      this.offerError.set(saveError instanceof Error ? saveError.message : 'Unable to save this job offer.');
    } finally {
      this.offerSaving.set(false);
    }
  }

  async deleteOffer(offer: JobOffer): Promise<void> {
    if (!window.confirm(`Delete “${offer.title}” permanently?`)) return;
    this.offerError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/admin/job-offers/${offer.id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to delete this job offer.'));
      if (this.editingOfferId() === offer.id) this.resetOfferForm();
      await this.refreshAdminOffers();
    } catch (deleteError) {
      this.offerError.set(deleteError instanceof Error ? deleteError.message : 'Unable to delete this job offer.');
    }
  }

  private async refreshAdminOffers(): Promise<void> {
    const [offersResponse, statsResponse] = await Promise.all([
      this.authorizedFetch(`${this.apiUrl}/admin/job-offers`),
      this.authorizedFetch(`${this.apiUrl}/admin/stats`),
    ]);
    if (!offersResponse.ok || !statsResponse.ok) throw new Error('Unable to refresh job offers.');
    this.adminOffers.set(await offersResponse.json() as JobOffer[]);
    this.adminStats.set(await statsResponse.json() as AdminStats);
    this.jobOffers.set(this.adminOffers());
  }

  showAnalyzer(): void { this.currentView.set('analyzer'); }
  get apiUrl(): string { return '/api'; }
  score(value: number | undefined): number { return Math.round((value ?? 0) * 100); }
  compatibilityScore(): number { return Math.round((this.result()?.compatibility_score ?? 0) * 1000) / 10; }

  private async authorizedFetch(url: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers);
    headers.set('Authorization', `Bearer ${this.token()}`);
    const response = await fetch(url, { ...init, headers });
    if (response.status === 401) this.logout();
    return response;
  }

  private async responseError(response: Response, fallback: string): Promise<string> {
    const payload = await response.json().catch(() => null) as { detail?: string; message?: string } | null;
    return payload?.detail ?? payload?.message ?? fallback;
  }

  private readStoredUser(): AuthUser | null {
    try {
      const stored = localStorage.getItem('careerlens_user');
      return stored ? JSON.parse(stored) as AuthUser : null;
    } catch { return null; }
  }
}
