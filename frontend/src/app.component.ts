import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface AnalysisResult {
  compatibility_score: number;
  confidence?: number;
  score_breakdown?: { skills?: number; semantic?: number; experience?: number; education?: number };
  models?: { tfidf?: { similarity?: number }; embeddings?: { similarity?: number } };
  skills?: { matching_skills?: Array<{ job_skill: string }>; missing_required_skills?: string[] };
  ai_explanation?: { summary?: string; recommendations?: string[]; cv_improvements?: string[]; experience_analysis?: string };
}

interface AuthUser {
  id: number;
  fullName: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

interface AuthPayload { token: string; user: AuthUser; }
interface AdminStats { users: number; analyses: number; jobOffers: number; applications: number; }

interface JobOffer {
  id: number;
  title: string;
  company: string;
  location: string;
  employmentType: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  active: boolean;
}

interface CandidateProfile { id: number; fullName: string; email: string; headline: string; savedCvText: string; skills: string; preferences: string; }
interface HistoryItem { id: number; compatibilityScore: number; jobOfferId: number | null; jobTitle: string; company: string; createdAt: string; }
interface ApplicationItem { id: number; jobOfferId: number; jobTitle: string; company: string; location: string; userId: number; candidateName: string; candidateEmail: string; status: string; coverLetter: string; createdAt: string; updatedAt: string; }

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
  readonly currentView = signal<'analyzer' | 'jobs' | 'applications' | 'history' | 'profile' | 'admin'>('analyzer');
  readonly uploadingCv = signal(false);
  readonly uploadingJob = signal(false);
  readonly cvFilename = signal('');
  readonly jobFilename = signal('');
  readonly adminStats = signal<AdminStats | null>(null);
  readonly adminUsers = signal<AuthUser[]>([]);
  readonly adminLoading = signal(false);
  readonly adminSection = signal<'overview' | 'applications' | 'offers' | 'users'>('overview');
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
  jobSearch = '';
  jobType = '';
  readonly profile = signal<CandidateProfile | null>(null);
  readonly profileLoading = signal(false);
  readonly profileSaving = signal(false);
  readonly profileMessage = signal('');
  readonly history = signal<HistoryItem[]>([]);
  readonly historyLoading = signal(false);
  readonly applications = signal<ApplicationItem[]>([]);
  readonly applicationsLoading = signal(false);
  readonly applicationsRefreshing = signal(false);
  readonly applicationsLastRefresh = signal<Date | null>(null);
  readonly adminApplications = signal<ApplicationItem[]>([]);
  readonly applicationSaving = signal(false);
  readonly applicationMessage = signal('');
  readonly adminApplicationMessage = signal('');
  readonly applicationStatusUpdatingId = signal<number | null>(null);
  readonly favoriteOfferIds = signal<number[]>([]);
  readonly favoritesOnly = signal(false);
  coverLetter = '';
  private applicationsRefreshTimer: number | null = null;
  profileFullName = '';
  profileHeadline = '';
  profileCv = '';
  profileSkills = '';
  profilePreferences = '';
  readonly visibleResult = computed(() => this.currentView() === 'analyzer' || this.currentView() === 'jobs' ? this.result() : null);
  readonly applicationChartData = computed(() => {
    const definitions = [
      { status: 'SENT', label: 'Envoyées', color: '#4d73e8' },
      { status: 'REVIEWED', label: 'Consultées', color: '#37a2da' },
      { status: 'INTERVIEW', label: 'Entretiens', color: '#8b62dc' },
      { status: 'ACCEPTED', label: 'Acceptées', color: '#28a77b' },
      { status: 'REJECTED', label: 'Refusées', color: '#e26767' },
      { status: 'WITHDRAWN', label: 'Retirées', color: '#a5adba' },
    ];
    return definitions.map(item => ({ ...item, count: this.adminApplications().filter(application => application.status === item.status).length }));
  });
  readonly activeOfferCount = computed(() => this.adminOffers().filter(offer => offer.active).length);
  readonly archivedOfferCount = computed(() => this.adminOffers().filter(offer => !offer.active).length);

  applicationChartGradient(): string {
    const data = this.applicationChartData();
    const total = Math.max(1, data.reduce((sum, item) => sum + item.count, 0));
    let cursor = 0;
    const stops = data.filter(item => item.count > 0).map(item => {
      const start = cursor;
      cursor += (item.count / total) * 100;
      return `${item.color} ${start}% ${cursor}%`;
    });
    return stops.length ? `conic-gradient(${stops.join(',')})` : 'conic-gradient(#e7ebf3 0 100%)';
  }

  metricBarHeight(value: number): number {
    const stats = this.adminStats();
    const values = [stats?.users ?? 0, stats?.jobOffers ?? 0, stats?.applications ?? 0, stats?.analyses ?? 0];
    const maximum = Math.max(1, ...values);
    return Math.max(12, Math.round((value / maximum) * 100));
  }

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
    this.stopApplicationsRefresh();
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
        body: JSON.stringify({ cv_text: this.cvText, job_description: this.jobDescription, job_offer_id: this.selectedOffer()?.id ?? null }),
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

  async uploadDocument(event: Event, target: 'cv' | 'job' | 'profile'): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const uploading = target === 'job' ? this.uploadingJob : this.uploadingCv;
    uploading.set(true);
    this.error.set('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await this.authorizedFetch(`${this.apiUrl}/documents/extract`, { method: 'POST', body: formData });
      if (!response.ok) throw new Error(await this.responseError(response, 'The document could not be imported.'));
      const payload = await response.json() as { text: string; filename: string };
      if (target === 'profile') {
        this.profileCv = payload.text;
        this.cvFilename.set(payload.filename);
      } else if (target === 'cv') {
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
    this.adminSection.set('overview');
    this.adminLoading.set(true);
    this.error.set('');
    try {
      const [statsResponse, usersResponse, offersResponse, applicationsResponse] = await Promise.all([
        this.authorizedFetch(`${this.apiUrl}/admin/stats`),
        this.authorizedFetch(`${this.apiUrl}/admin/users`),
        this.authorizedFetch(`${this.apiUrl}/admin/job-offers`),
        this.authorizedFetch(`${this.apiUrl}/admin/applications`),
      ]);
      if (!statsResponse.ok || !usersResponse.ok || !offersResponse.ok || !applicationsResponse.ok) throw new Error('Unable to load the admin dashboard.');
      this.adminStats.set(await statsResponse.json() as AdminStats);
      this.adminUsers.set(await usersResponse.json() as AuthUser[]);
      this.adminOffers.set(await offersResponse.json() as JobOffer[]);
      this.adminApplications.set(await applicationsResponse.json() as ApplicationItem[]);
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
    await Promise.all([this.loadJobOffers(), this.loadFavorites()]);
    if (!this.profile()) {
      const profileResponse = await this.authorizedFetch(`${this.apiUrl}/profile`);
      if (profileResponse.ok) this.profile.set(await profileResponse.json() as CandidateProfile);
    }
  }

  filteredJobOffers(): JobOffer[] {
    const query = this.jobSearch.trim().toLowerCase();
    return this.jobOffers().filter(offer => {
      const text = `${offer.title} ${offer.company} ${offer.location} ${offer.description}`.toLowerCase();
      return (!query || text.includes(query))
        && (!this.jobType || offer.employmentType === this.jobType)
        && (!this.favoritesOnly() || this.isFavorite(offer.id));
    });
  }

  async loadFavorites(): Promise<void> {
    const response = await this.authorizedFetch(`${this.apiUrl}/favorites`);
    if (response.ok) this.favoriteOfferIds.set(await response.json() as number[]);
  }

  isFavorite(offerId: number): boolean { return this.favoriteOfferIds().includes(offerId); }

  async toggleFavorite(offer: JobOffer, event?: Event): Promise<void> {
    event?.stopPropagation();
    this.error.set('');
    const response = await this.authorizedFetch(`${this.apiUrl}/favorites/${offer.id}`, { method: 'POST' });
    if (!response.ok) { this.error.set(await this.responseError(response, 'Unable to update favorites.')); return; }
    const payload = await response.json() as { favorite: boolean; jobOfferId: number };
    this.favoriteOfferIds.update(ids => payload.favorite
      ? Array.from(new Set([...ids, payload.jobOfferId]))
      : ids.filter(id => id !== payload.jobOfferId));
  }

  async openProfile(): Promise<void> {
    this.currentView.set('profile');
    this.profileLoading.set(true);
    this.error.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/profile`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load your profile.'));
      const profile = await response.json() as CandidateProfile;
      this.profile.set(profile);
      this.profileFullName = profile.fullName;
      this.profileHeadline = profile.headline;
      this.profileCv = profile.savedCvText;
      this.profileSkills = profile.skills;
      this.profilePreferences = profile.preferences;
    } catch (profileError) {
      this.error.set(profileError instanceof Error ? profileError.message : 'Unable to load your profile.');
    } finally { this.profileLoading.set(false); }
  }

  async saveProfile(): Promise<void> {
    this.profileSaving.set(true);
    this.profileMessage.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/profile`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
          fullName: this.profileFullName, headline: this.profileHeadline, savedCvText: this.profileCv,
          skills: this.profileSkills, preferences: this.profilePreferences,
        }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to save your profile.'));
      const profile = await response.json() as CandidateProfile;
      this.profile.set(profile);
      const current = this.user();
      if (current) {
        const updated = { ...current, fullName: profile.fullName };
        this.user.set(updated); localStorage.setItem('careerlens_user', JSON.stringify(updated));
      }
      this.profileMessage.set('Profile and CV saved successfully.');
    } catch (saveError) {
      this.error.set(saveError instanceof Error ? saveError.message : 'Unable to save your profile.');
    } finally { this.profileSaving.set(false); }
  }

  useSavedCv(): void {
    const saved = this.profile()?.savedCvText || this.profileCv;
    if (!saved) { this.error.set('Save a CV in your profile first.'); return; }
    this.cvText = saved;
    this.cvFilename.set('Saved profile CV');
  }

  async openHistory(): Promise<void> {
    this.currentView.set('history');
    this.historyLoading.set(true);
    this.error.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/history`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load your history.'));
      this.history.set(await response.json() as HistoryItem[]);
    } catch (historyError) {
      this.error.set(historyError instanceof Error ? historyError.message : 'Unable to load your history.');
    } finally { this.historyLoading.set(false); }
  }

  async openApplications(): Promise<void> {
    this.currentView.set('applications');
    await this.loadApplications(true);
    this.startApplicationsRefresh();
  }

  async refreshApplications(): Promise<void> {
    await this.loadApplications(false);
  }

  private async loadApplications(showLoader: boolean): Promise<void> {
    if (showLoader) this.applicationsLoading.set(true);
    else this.applicationsRefreshing.set(true);
    this.error.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/applications`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load applications.'));
      this.applications.set(await response.json() as ApplicationItem[]);
      this.applicationsLastRefresh.set(new Date());
    } catch (applicationError) {
      this.error.set(applicationError instanceof Error ? applicationError.message : 'Unable to load applications.');
    } finally {
      this.applicationsLoading.set(false);
      this.applicationsRefreshing.set(false);
    }
  }

  private startApplicationsRefresh(): void {
    if (this.applicationsRefreshTimer !== null) return;
    this.applicationsRefreshTimer = window.setInterval(() => {
      if (this.currentView() === 'applications' && this.token()) void this.loadApplications(false);
    }, 5000);
  }

  private stopApplicationsRefresh(): void {
    if (this.applicationsRefreshTimer === null) return;
    window.clearInterval(this.applicationsRefreshTimer);
    this.applicationsRefreshTimer = null;
  }

  async applyToSelectedOffer(): Promise<void> {
    const offer = this.selectedOffer();
    if (!offer || this.applicationSaving()) return;
    this.applicationSaving.set(true); this.applicationMessage.set(''); this.error.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/applications`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ jobOfferId: offer.id, coverLetter: this.coverLetter }) });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to submit application.'));
      this.applicationMessage.set('Application submitted successfully.');
    } catch (applyError) { this.error.set(applyError instanceof Error ? applyError.message : 'Unable to submit application.'); }
    finally { this.applicationSaving.set(false); }
  }

  async withdrawApplication(application: ApplicationItem): Promise<void> {
    if (!window.confirm(`Withdraw your application for “${application.jobTitle}”?`)) return;
    const response = await this.authorizedFetch(`${this.apiUrl}/applications/${application.id}`, { method: 'DELETE' });
    if (!response.ok) { this.error.set(await this.responseError(response, 'Unable to withdraw application.')); return; }
    await this.openApplications();
  }

  async updateApplicationStatus(application: ApplicationItem, status: string): Promise<void> {
    this.adminApplicationMessage.set('');
    this.error.set('');
    this.applicationStatusUpdatingId.set(application.id);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/admin/applications/${application.id}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status }) });
      if (!response.ok) throw new Error(await this.responseError(response, 'Impossible de modifier le statut.'));
      const updated = await response.json() as ApplicationItem;
      this.adminApplications.update(items => items.map(item => item.id === updated.id ? updated : item));
      this.adminApplicationMessage.set(`Le statut de ${updated.candidateName} est maintenant « ${this.applicationStatusLabel(updated.status)} ».`);
    } catch (statusError) {
      this.error.set(statusError instanceof Error ? statusError.message : 'Impossible de modifier le statut.');
    } finally {
      this.applicationStatusUpdatingId.set(null);
    }
  }

  applicationStatusLabel(status: string): string {
    return ({ SENT: 'Envoyée', REVIEWED: 'Consultée', INTERVIEW: 'Entretien', ACCEPTED: 'Acceptée', REJECTED: 'Refusée', WITHDRAWN: 'Retirée' } as Record<string, string>)[status] ?? status;
  }

  applicationStatusMessage(status: string): string {
    return ({
      SENT: 'Votre candidature a bien été transmise à l’entreprise.',
      REVIEWED: 'Votre candidature a été consultée par le recruteur.',
      INTERVIEW: 'Bonne nouvelle : vous avez été sélectionné(e) pour un entretien.',
      ACCEPTED: 'Félicitations ! Votre candidature a été acceptée.',
      REJECTED: 'Votre candidature n’a malheureusement pas été retenue.',
      WITHDRAWN: 'Vous avez retiré cette candidature.',
    } as Record<string, string>)[status] ?? 'Le statut de votre candidature a été mis à jour.';
  }

  applicationStatusIcon(status: string): string {
    return ({ SENT: '↗', REVIEWED: '◉', INTERVIEW: '◷', ACCEPTED: '✓', REJECTED: '×', WITHDRAWN: '−' } as Record<string, string>)[status] ?? '•';
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
    this.applicationMessage.set('');
    this.coverLetter = '';
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

  async toggleOfferActive(offer: JobOffer): Promise<void> {
    this.offerError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/admin/job-offers/${offer.id}/active`, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !offer.active }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to change publication status.'));
      await this.refreshAdminOffers();
    } catch (statusError) {
      this.offerError.set(statusError instanceof Error ? statusError.message : 'Unable to change publication status.');
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
    this.jobOffers.set(this.adminOffers().filter(offer => offer.active));
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
