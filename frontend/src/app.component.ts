import { CommonModule } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { animate, stagger } from 'motion';
import { ThreeSceneComponent } from './three-scene.component';
import { CareerVisualComponent } from './career-visual.component';

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
interface AdminStats { users: number; analyses: number; jobOffers: number; }
interface AdminInterviewAnswer { question: string; type: string; score: number | null; feedback: string; strengths: string[]; improvements: string[]; }
interface AdminInterviewResult { id: number; candidateName: string; candidateEmail: string; offerTitle: string; company: string; status: 'COMPLETED' | 'IN_PROGRESS'; score: number | null; answeredQuestions: number; totalQuestions: number; createdAt: string; answers: AdminInterviewAnswer[]; strengths: string[]; weaknesses: string[]; recommendations: string[]; }

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

interface JobMatch { offer: JobOffer; compatibility_score: number; matching_skills: Array<{ candidate_skill: string; job_skill: string }>; missing_required_skills: string[]; }

interface InterviewQuestion { type: string; question: string; }
interface InterviewEvaluation { question: string; type: string; answer: string; score: number; feedback: string; strengths: string[]; improvements: string[]; }
interface InterviewSummary { score: number; strengths: string[]; weaknesses: string[]; recommendations: string[]; }
interface InterviewSession { id: number; offerId: number; offerTitle: string; company: string; questions: InterviewQuestion[]; profile: { matching_skills: string[]; skill_gaps: string[]; summary: string }; evaluations: InterviewEvaluation[]; currentQuestion: number; completed: boolean; summary: InterviewSummary | null; createdAt: string; evaluation?: InterviewEvaluation; }
interface CandidateProfile { id: number; fullName: string; email: string; headline: string; savedCvText: string; skills: string; preferences: string; }
interface CodeDebugFinding { bug_type: string; severity: string; line: number; message: string; recommendation: string; }
interface CodeDebugResult { module_label: string; bug_type: string; analysis_method: string; code_was_executed: boolean; summary: string; findings: CodeDebugFinding[]; }
interface ProgressGoal { id: number; skillName: string; objective: string; activity: string; exercise: string; status: 'TO_DO' | 'IN_PROGRESS' | 'DONE'; statusLabel: string; sortOrder: number; createdAt: string; updatedAt: string; }
interface ProgressPlan { id: number; analysisId: number | null; jobOfferId: number | null; title: string; summary: string; progressPercentage: number; totalGoals: number; completedGoals: number; goals: ProgressGoal[]; createdAt: string; updatedAt: string; }
interface ApplicationKit { id: number; jobOfferId: number; offerTitle: string; company: string; adaptedCv: string; coverLetter: string; matchedSkills: string[]; missingSkills: string[]; createdAt: string; updatedAt: string; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, ThreeSceneComponent, CareerVisualComponent],
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
  readonly publicView = signal<'landing' | 'auth'>('landing');
  // Always open on the public home page, even when a valid session is restored.
  readonly marketingPreview = signal(true);
  readonly authLoading = signal(false);
  readonly authError = signal('');
  readonly currentView = signal<'analyzer' | 'jobs' | 'interviews' | 'profile' | 'progress' | 'applications' | 'debug' | 'admin'>('analyzer');
  readonly sidebarCollapsed = signal(false);
  readonly mobileSidebarOpen = signal(false);
  readonly uploadingCv = signal(false);
  readonly uploadingJob = signal(false);
  readonly cvFilename = signal('');
  readonly jobFilename = signal('');
  readonly adminStats = signal<AdminStats | null>(null);
  readonly adminUsers = signal<AuthUser[]>([]);
  readonly adminInterviews = signal<AdminInterviewResult[]>([]);
  readonly adminLoading = signal(false);
  readonly adminSection = signal<'overview' | 'offers' | 'users'>('overview');
  readonly jobOffers = signal<JobOffer[]>([]);
  readonly jobMatches = signal<JobMatch[]>([]);
  readonly matchingJobsLoading = signal(false);
  readonly matchingJobsError = signal('');
  readonly matchingJobsTotal = signal(0);
  readonly matchingJobsSearched = signal(false);
  readonly jobSearch = signal('');
  readonly jobCategory = signal('All domains');
  readonly jobPage = signal(0);
  readonly adminOfferPage = signal(0);
  readonly jobPageSize = 12;
  readonly adminOfferPageSize = 10;
  readonly jobCategories = computed(() => ['All domains', ...new Set(this.jobOffers().map(offer => this.getOfferCategory(offer)).filter(Boolean).sort())]);
  filteredJobOffers(): JobOffer[] {
    const query = this.jobSearch().trim().toLocaleLowerCase();
    return this.jobOffers().filter(offer => {
      const matchesCategory = this.jobCategory() === 'All domains' || this.getOfferCategory(offer) === this.jobCategory();
      const matchesType = !this.jobType || offer.employmentType === this.jobType;
      const matchesFavorite = !this.favoritesOnly() || this.isFavorite(offer.id);
      const searchable = `${offer.title} ${offer.company} ${offer.location} ${offer.employmentType} ${offer.description}`.toLocaleLowerCase();
      return matchesCategory && matchesType && matchesFavorite && (!query || searchable.includes(query));
    });
  }
  readonly jobPageCount = computed(() => Math.max(1, Math.ceil(this.filteredJobOffers().length / this.jobPageSize)));
  readonly displayedJobOffers = computed(() => this.filteredJobOffers().slice(this.jobPage() * this.jobPageSize, (this.jobPage() + 1) * this.jobPageSize));
  readonly displayedAdminOffers = computed(() => this.adminOffers().slice(this.adminOfferPage() * this.adminOfferPageSize, (this.adminOfferPage() + 1) * this.adminOfferPageSize));
  readonly adminOfferPageCount = computed(() => Math.max(1, Math.ceil(this.adminOffers().length / this.adminOfferPageSize)));
  readonly offersLoading = signal(false);
  readonly interviewSession = signal<InterviewSession | null>(null);
  readonly interviewLoading = signal(false);
  readonly interviewError = signal('');
  readonly interviewAnswer = signal('');
  readonly interviewCv = signal('');
  readonly interviewOfferId = signal<number | null>(null);
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
  jobType = '';
  readonly profile = signal<CandidateProfile | null>(null);
  readonly profileLoading = signal(false);
  readonly profileSaving = signal(false);
  readonly profileMessage = signal('');
  readonly analysisId = signal<number | null>(null);
  readonly progressPlans = signal<ProgressPlan[]>([]);
  readonly progressPlansLoading = signal(false);
  readonly progressPlansError = signal('');
  readonly selectedProgressPlan = signal<ProgressPlan | null>(null);
  readonly planGenerating = signal(false);
  readonly applicationKits = signal<ApplicationKit[]>([]);
  readonly selectedKit = signal<ApplicationKit | null>(null);
  readonly kitLoading = signal(false);
  readonly kitError = signal('');
  readonly kitLetterDraft = signal('');
  readonly favoriteOfferIds = signal<number[]>([]);
  readonly debugModule = signal<'matching' | 'offers' | 'interviews' | 'profile'>('matching');
  readonly debugLanguage = signal<'python' | 'java' | 'javascript' | 'typescript'>('python');
  readonly debugCode = signal('');
  readonly debugExpectedBehavior = signal('');
  readonly debugResult = signal<CodeDebugResult | null>(null);
  readonly debugLoading = signal(false);
  readonly debugError = signal('');
  readonly favoritesOnly = signal(false);
  profileFullName = '';
  profileHeadline = '';
  profileCv = '';
  profileSkills = '';
  profilePreferences = '';
  readonly visibleResult = computed(() => this.currentView() === 'analyzer' || this.currentView() === 'jobs' ? this.result() : null);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly continuousMotionElements = new WeakSet<Element>();
  private readonly viewMotion = effect(() => {
    this.publicView();
    this.currentView();
    this.adminSection();
    window.setTimeout(() => this.animateCurrentView(), 0);
  });
  readonly activeOfferCount = computed(() => this.adminOffers().filter(offer => offer.active).length);
  readonly archivedOfferCount = computed(() => this.adminOffers().filter(offer => !offer.active).length);

  metricBarHeight(value: number): number {
    const stats = this.adminStats();
    const values = [stats?.users ?? 0, stats?.jobOffers ?? 0, this.adminInterviews().length, stats?.analyses ?? 0];
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
      this.profile.set(null);
      this.profileCv = '';
      this.cvText = '';
      this.cvFilename.set('');
      this.publicView.set('landing');
      this.marketingPreview.set(false);
      this.password = '';
      this.profileLoading.set(true);
      void this.loadProfileData()
        .catch(profileError => this.error.set(profileError instanceof Error ? profileError.message : 'Unable to load your profile.'))
        .finally(() => this.profileLoading.set(false));
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

  openAuth(mode: 'login' | 'register'): void {
    this.authMode.set(mode);
    this.authError.set('');
    this.publicView.set('auth');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  showLanding(): void {
    this.publicView.set('landing');
    this.marketingPreview.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  openMarketing(): void {
    this.publicView.set('landing');
    this.marketingPreview.set(true);
    this.closeMobileSidebar();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  returnToWorkspace(): void {
    this.marketingPreview.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  openFromLanding(view: 'analyzer' | 'jobs' | 'profile' | 'progress' | 'admin'): void {
    if (!this.user()) { this.openAuth('login'); return; }
    this.returnToWorkspace();
    window.setTimeout(() => {
      if (view === 'analyzer') this.showAnalyzer();
      else if (view === 'jobs') void this.openJobs();
      else if (view === 'profile') void this.openProfile();
      else if (view === 'progress') void this.openProgressPlans();
      else if (view === 'admin' && this.user()?.role === 'ADMIN') void this.openAdmin();
    }, 0);
  }

  logout(): void {
    localStorage.removeItem('careerlens_token');
    localStorage.removeItem('careerlens_user');
    this.token.set('');
    this.user.set(null);
    this.result.set(null);
    this.selectedOffer.set(null);
    this.cvText = '';
    this.jobDescription = '';
    this.cvFilename.set('');
    this.profile.set(null);
    this.profileCv = '';
    this.profileFullName = '';
    this.profileHeadline = '';
    this.profileSkills = '';
    this.profilePreferences = '';
    this.analysisId.set(null);
    this.progressPlans.set([]);
    this.selectedProgressPlan.set(null);
    this.progressPlansError.set('');
    this.jobMatches.set([]);
    this.interviewCv.set('');
    this.interviewSession.set(null);
    this.currentView.set('analyzer');
    this.publicView.set('landing');
    this.marketingPreview.set(false);
  }

  async analyze(): Promise<void> {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    this.analysisId.set(null);
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
      const payload = await response.json() as { result: AnalysisResult; analysis_id?: number | null };
      if (!payload.result) throw new Error('The AI service returned an invalid response.');
      this.result.set(payload.result);
      this.analysisId.set(payload.analysis_id ?? null);
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
        this.result.set(null);
        if (this.currentView() === 'analyzer') this.resetJobMatches();
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
    this.adminOfferPage.set(0);
    this.adminLoading.set(true);
    this.error.set('');
    try {
      const [statsResponse, usersResponse, offersResponse, interviewsResponse] = await Promise.all([
        this.authorizedFetch(`${this.apiUrl}/admin/stats`),
        this.authorizedFetch(`${this.apiUrl}/admin/users`),
        this.authorizedFetch(`${this.apiUrl}/admin/job-offers`),
        this.authorizedFetch(`${this.apiUrl}/admin/interviews`),
      ]);
      if (!statsResponse.ok || !usersResponse.ok || !offersResponse.ok || !interviewsResponse.ok) throw new Error('Unable to load the admin dashboard.');
      this.adminStats.set(await statsResponse.json() as AdminStats);
      this.adminUsers.set(await usersResponse.json() as AuthUser[]);
      this.adminOffers.set(await offersResponse.json() as JobOffer[]);
      this.adminInterviews.set(await interviewsResponse.json() as AdminInterviewResult[]);
      this.adminOfferPage.set(0);
    } catch (adminError) {
      this.error.set(adminError instanceof Error ? adminError.message : 'Admin dashboard failed to load.');
    } finally {
      this.adminLoading.set(false);
    }
  }

  async openJobs(): Promise<void> {
    this.currentView.set('jobs');
    this.jobPage.set(0);
    this.selectedOffer.set(null);
    this.result.set(null);
    this.error.set('');
    await Promise.all([this.loadJobOffers(), this.loadFavorites()]);
    if (!this.profile()) {
      const profileResponse = await this.authorizedFetch(`${this.apiUrl}/profile`);
      if (profileResponse.ok) this.profile.set(await profileResponse.json() as CandidateProfile);
    }
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
    try { await this.loadProfileData(); }
    catch (profileError) { this.error.set(profileError instanceof Error ? profileError.message : 'Unable to load your profile.'); }
    finally { this.profileLoading.set(false); }
  }

  private async loadProfileData(): Promise<CandidateProfile> {
    const response = await this.authorizedFetch(`${this.apiUrl}/profile`);
    if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load your profile.'));
    const profile = await response.json() as CandidateProfile;
    this.profile.set(profile);
    this.profileFullName = profile.fullName;
    this.profileHeadline = profile.headline;
    this.profileCv = profile.savedCvText;
    this.profileSkills = profile.skills;
    this.profilePreferences = profile.preferences;
    return profile;
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

  async useSavedCv(): Promise<void> {
    this.error.set('');
    const needsProfileLoad = !this.profile();
    if (needsProfileLoad) this.profileLoading.set(true);
    try {
      const profile = this.profile() ?? await this.loadProfileData();
      const saved = profile.savedCvText.trim();
      if (!saved) {
        this.error.set('No CV is saved in your profile yet. Add one in Profile, then come back here.');
        return;
      }
      this.cvText = saved;
      this.cvFilename.set('Saved profile CV');
      if (this.currentView() === 'analyzer') this.resetJobMatches();
      else this.result.set(null);
    } catch (profileError) {
      this.error.set(profileError instanceof Error ? profileError.message : 'Unable to load your saved CV.');
    } finally {
      if (needsProfileLoad) this.profileLoading.set(false);
    }
  }

  selectAdminSection(section: 'overview' | 'offers' | 'users'): void {
    this.adminSection.set(section);
  }

  toggleSidebar(): void { this.sidebarCollapsed.update(value => !value); }
  closeMobileSidebar(): void { this.mobileSidebarOpen.set(false); }

  currentViewLabel(): string {
    return ({ analyzer: 'Matching IA', jobs: 'Offres d’emploi', interviews: 'Interview practice', profile: 'Mon profil', progress: 'Mon plan de progression', applications: 'My applications', debug: 'AI Code Debugger', admin: 'Administration' } as const)[this.currentView()];
  }

  sceneVariant(): 'analyzer' | 'jobs' | 'profile' | 'admin' | 'progress' {
    const view = this.currentView();
    if (view === 'interviews' || view === 'applications' || view === 'debug') return 'jobs';
    if (view === 'progress') return 'progress';
    return view;
  }

  private animateCurrentView(): void {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const root = this.host.nativeElement as HTMLElement;
    const targets = Array.from(root.querySelectorAll<HTMLElement>(
      '.landing-hero-copy > *, .landing-visual, .landing-logo-strip > *, .landing-section-heading, .landing-steps > *, .landing-bento > *, .landing-cta > *, .auth-brand, .auth-message > *, .auth-proof, .auth-card > *, main > section:not(.processing) > .jobs-heading, main > section:not(.processing) > .section-heading, .hero > *, .analysis-form > *, .job-filters, .job-grid > *, .offer-detail, .job-match-card, .history-list > *, .profile-layout > *, .admin-main > .admin-hero, .admin-main .stat-grid > *, .dashboard-charts > *, .admin-overview-actions > *, .admin-offers-section, .admin-main > .users-card'
    )).filter(element => element.offsetParent !== null);
    if (!targets.length) return;
    animate(targets, { opacity: [0, 1], y: [18, 0], scale: [.985, 1] }, {
      duration: .5,
      delay: stagger(.035),
      ease: [.16, 1, .3, 1],
    });
    const floatingPieces = root.querySelectorAll<HTMLElement>('.landing-float-piece');
    if (floatingPieces.length) animate(floatingPieces, {
      y: [-8, 12, -8],
      rotate: [-5, 7, -5],
    }, { duration: 5.5, delay: stagger(.35), repeat: Infinity, ease: 'easeInOut' });
    const orbitRings = root.querySelectorAll<HTMLElement>('.landing-orbit-ring');
    if (orbitRings.length) animate(orbitRings, { rotate: [0, 360] }, {
      duration: 24,
      delay: stagger(1.5),
      repeat: Infinity,
      ease: 'linear',
    });
    const authPieces = root.querySelectorAll<HTMLElement>('.auth-float-piece');
    if (authPieces.length) animate(authPieces, {
      y: [-7, 11, -7],
      rotate: [-4, 5, -4],
    }, { duration: 5, delay: stagger(.45), repeat: Infinity, ease: 'easeInOut' });
    const authOrbits = root.querySelectorAll<HTMLElement>('.auth-orbit');
    if (authOrbits.length) animate(authOrbits, { rotate: [0, 360] }, {
      duration: 28,
      delay: stagger(2),
      repeat: Infinity,
      ease: 'linear',
    });
    const workspacePieces = Array.from(root.querySelectorAll<HTMLElement>('.workspace-float-piece')).filter(element => {
      if (this.continuousMotionElements.has(element)) return false;
      this.continuousMotionElements.add(element); return true;
    });
    if (workspacePieces.length) animate(workspacePieces, {
      y: [-6, 10, -6],
      rotate: [-3, 5, -3],
    }, { duration: 6, delay: stagger(.55), repeat: Infinity, ease: 'easeInOut' });
    const workspaceOrbits = Array.from(root.querySelectorAll<HTMLElement>('.workspace-orbit')).filter(element => {
      if (this.continuousMotionElements.has(element)) return false;
      this.continuousMotionElements.add(element); return true;
    });
    if (workspaceOrbits.length) animate(workspaceOrbits, { rotate: [0, 360] }, {
      duration: 32,
      delay: stagger(2.5),
      repeat: Infinity,
      ease: 'linear',
    });
  }

  async openProgressPlans(): Promise<void> {
    this.currentView.set('progress');
    this.progressPlansError.set('');
    this.selectedProgressPlan.set(null);
    await this.loadProgressPlans();
  }

  async loadProgressPlans(): Promise<void> {
    this.progressPlansLoading.set(true);
    this.progressPlansError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/progress-plans`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Impossible de charger votre plan de progression.'));
      const plans = await response.json() as ProgressPlan[];
      this.progressPlans.set(plans ?? []);
      if (plans.length) {
        this.selectedProgressPlan.set(plans[0]);
      }
    } catch (error) {
      this.progressPlansError.set(error instanceof Error ? error.message : 'Impossible de charger votre plan de progression.');
    } finally {
      this.progressPlansLoading.set(false);
    }
  }

  async openApplicationKits(): Promise<void> {
    this.currentView.set('applications');
    this.kitError.set('');
    this.selectedKit.set(null);
    this.kitLetterDraft.set('');
    await this.loadApplicationKits();
  }

  async loadApplicationKits(): Promise<void> {
    this.kitLoading.set(true);
    this.kitError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/application-kits`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load your application kits.'));
      const kits = await response.json() as ApplicationKit[];
      this.applicationKits.set(kits ?? []);
      if (kits.length) {
        this.selectedKit.set(kits[0]);
        this.kitLetterDraft.set(kits[0].coverLetter);
      }
    } catch (error) {
      this.kitError.set(error instanceof Error ? error.message : 'Unable to load your application kits.');
    } finally {
      this.kitLoading.set(false);
    }
  }

  selectKit(kit: ApplicationKit): void {
    this.selectedKit.set(kit);
    this.kitLetterDraft.set(kit.coverLetter);
  }

  async generateKitForOffer(offer: JobOffer): Promise<void> {
    if (this.kitLoading()) return;
    this.kitLoading.set(true);
    this.kitError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/application-kits`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobOfferId: offer.id }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to generate your application kit.'));
      const kit = await response.json() as ApplicationKit;
      this.applicationKits.update(current => [kit, ...current.filter(item => item.id !== kit.id)]);
      this.selectedKit.set(kit);
      this.kitLetterDraft.set(kit.coverLetter);
      this.currentView.set('applications');
    } catch (error) {
      this.kitError.set(error instanceof Error ? error.message : 'Unable to generate your application kit.');
    } finally {
      this.kitLoading.set(false);
    }
  }

  async saveCoverLetter(): Promise<void> {
    const kit = this.selectedKit();
    if (!kit || this.kitLoading()) return;
    this.kitLoading.set(true);
    this.kitError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/application-kits/${kit.id}/cover-letter`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coverLetter: this.kitLetterDraft() }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to save your cover letter.'));
      const updated = await response.json() as ApplicationKit;
      this.applicationKits.update(current => current.map(item => item.id === updated.id ? updated : item));
      this.selectedKit.set(updated);
      this.kitLetterDraft.set(updated.coverLetter);
    } catch (error) {
      this.kitError.set(error instanceof Error ? error.message : 'Unable to save your cover letter.');
    } finally {
      this.kitLoading.set(false);
    }
  }

  async deleteKit(kit: ApplicationKit): Promise<void> {
    if (this.kitLoading() || !window.confirm('Delete this application kit?')) return;
    this.kitLoading.set(true);
    this.kitError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/application-kits/${kit.id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to delete your application kit.'));
      this.applicationKits.update(current => current.filter(item => item.id !== kit.id));
      if (this.selectedKit()?.id === kit.id) {
        const remaining = this.applicationKits()[0] ?? null;
        this.selectedKit.set(remaining);
        this.kitLetterDraft.set(remaining?.coverLetter ?? '');
      }
    } catch (error) {
      this.kitError.set(error instanceof Error ? error.message : 'Unable to delete your application kit.');
    } finally {
      this.kitLoading.set(false);
    }
  }

  async copyText(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      this.kitError.set('Unable to copy the text.');
    }
  }

  async createProgressPlanFromAnalysis(): Promise<void> {
    const missingSkills = this.result()?.skills?.missing_required_skills ?? [];
    if (!missingSkills.length) {
      this.progressPlansError.set('Aucune compétence manquante détectée pour générer un plan personnalisé.');
      return;
    }
    this.planGenerating.set(true);
    this.progressPlansError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/progress-plans`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          analysisId: this.analysisId(),
          jobOfferId: this.selectedOffer()?.id ?? null,
          summary: 'Suggestions automatiques fondées sur les lacunes détectées lors du matching.',
          missingSkills,
        }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Le plan de progression n’a pas pu être créé.'));
      const created = await response.json() as ProgressPlan;
      this.progressPlans.update(current => [created, ...current.filter(plan => plan.id !== created.id)]);
      this.selectedProgressPlan.set(created);
      this.currentView.set('progress');
    } catch (error) {
      this.progressPlansError.set(error instanceof Error ? error.message : 'Le plan de progression n’a pas pu être créé.');
    } finally {
      this.planGenerating.set(false);
    }
  }

  async updateProgressGoalStatus(planId: number, goalId: number, status: 'TO_DO' | 'IN_PROGRESS' | 'DONE'): Promise<void> {
    const response = await this.authorizedFetch(`${this.apiUrl}/progress-plans/${planId}/goals/${goalId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    if (!response.ok) {
      this.progressPlansError.set(await this.responseError(response, 'Le statut du plan n’a pas pu être mis à jour.'));
      return;
    }
    const updated = await response.json() as ProgressPlan;
    this.progressPlans.update(current => current.map(plan => plan.id === updated.id ? updated : plan));
    const active = this.selectedProgressPlan();
    this.selectedProgressPlan.set(active && active.id === updated.id ? updated : active);
    this.progressPlansError.set('');
  }

  progressStatusLabel(status: 'TO_DO' | 'IN_PROGRESS' | 'DONE'): string {
    return ({ TO_DO: 'À faire', IN_PROGRESS: 'En cours', DONE: 'Terminé' } as const)[status];
  }

  progressPercentageForPlan(plan: ProgressPlan | null): number {
    if (!plan) return 0;
    return Math.round(plan.progressPercentage);
  }

  async openInterviews(): Promise<void> {
    this.currentView.set('interviews');
    this.interviewSession.set(null);
    this.interviewError.set('');
    await this.loadJobOffers();
  }

  async startInterview(): Promise<void> {
    if (this.interviewLoading()) return;
    const offerId = this.interviewOfferId();
    if (!offerId || !this.interviewCv().trim()) { this.interviewError.set('Select a job offer and provide your CV to start.'); return; }
    this.interviewLoading.set(true); this.interviewError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/interviews`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ offerId, cvText: this.interviewCv() }) });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to generate interview questions.'));
      this.interviewSession.set(await response.json() as InterviewSession);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (error) { this.interviewError.set(error instanceof Error ? error.message : 'Unable to start the interview.'); }
    finally { this.interviewLoading.set(false); }
  }

  async submitInterviewAnswer(): Promise<void> {
    const session = this.interviewSession();
    if (!session || !this.interviewAnswer().trim() || this.interviewLoading()) return;
    this.interviewLoading.set(true); this.interviewError.set('');
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/interviews/${session.id}/answers`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ answer: this.interviewAnswer() }) });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to evaluate this answer.'));
      const updated = await response.json() as InterviewSession;
      this.interviewSession.set(updated); this.interviewAnswer.set('');
    } catch (error) { this.interviewError.set(error instanceof Error ? error.message : 'Unable to submit this answer.'); }
    finally { this.interviewLoading.set(false); }
  }

  setInterviewCv(event: Event): void { this.interviewCv.set((event.target as HTMLTextAreaElement).value); }
  setInterviewOffer(event: Event): void { const value = (event.target as HTMLSelectElement).value; this.interviewOfferId.set(value ? Number(value) : null); }

  async uploadInterviewCv(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return;
    this.interviewLoading.set(true); this.interviewError.set('');
    try {
      const form = new FormData(); form.append('file', file);
      const response = await this.authorizedFetch(`${this.apiUrl}/documents/extract`, { method: 'POST', body: form });
      if (!response.ok) throw new Error(await this.responseError(response, 'The CV could not be imported.'));
      const payload = await response.json() as { text: string };
      this.interviewCv.set(payload.text);
    } catch (error) { this.interviewError.set(error instanceof Error ? error.message : 'The CV could not be imported.'); }
    finally { this.interviewLoading.set(false); input.value = ''; }
  }

  async loadJobOffers(): Promise<void> {
    this.offersLoading.set(true);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/job-offers`);
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to load job offers.'));
      this.jobOffers.set(await response.json() as JobOffer[]);
      this.jobPage.set(0);
    } catch (offersError) {
      this.error.set(offersError instanceof Error ? offersError.message : 'Unable to load job offers.');
    } finally {
      this.offersLoading.set(false);
    }
  }

  async runCareerAnalysis(): Promise<void> {
    if (!this.cvText.trim() || this.loading() || this.matchingJobsLoading()) return;
    this.error.set('');
    this.result.set(null);
    const matched = await this.findMatchingJobs(this.cvText);
    if (!matched) return;
    const bestOffer = this.jobMatches()[0]?.offer;
    if (!bestOffer) {
      this.error.set('Aucune offre publiée ne correspond à votre profil pour le moment.');
      return;
    }
    this.selectedOffer.set(bestOffer);
    this.jobDescription = bestOffer.description;
    await this.analyze();
  }

  private async findMatchingJobs(cvText: string): Promise<boolean> {
    if (!cvText.trim() || this.matchingJobsLoading()) return false;
    this.matchingJobsLoading.set(true);
    this.matchingJobsError.set('');
    this.jobMatches.set([]);
    this.matchingJobsTotal.set(0);
    this.matchingJobsSearched.set(false);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/job-offers/matches`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cvText }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Unable to find matching opportunities.'));
      const payload = await response.json() as { matches: JobMatch[]; total_offers: number };
      this.jobMatches.set(payload.matches ?? []);
      this.matchingJobsTotal.set(payload.total_offers ?? 0);
      this.matchingJobsSearched.set(true);
      return true;
    } catch (error) {
      this.matchingJobsError.set(error instanceof Error ? error.message : 'Unable to find matching opportunities.');
      return false;
    } finally {
      this.matchingJobsLoading.set(false);
    }
  }

  resetJobMatches(): void {
    this.result.set(null);
    this.selectedOffer.set(null);
    this.jobMatches.set([]);
    this.matchingJobsError.set('');
    this.matchingJobsTotal.set(0);
    this.matchingJobsSearched.set(false);
  }

  openMatchedOffer(offer: JobOffer): void {
    this.currentView.set('jobs');
    this.selectedOffer.set(offer);
    this.jobDescription = offer.description;
    this.result.set(null);
    this.error.set('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async practiceMatchedOffer(offer: JobOffer): Promise<void> {
    this.interviewOfferId.set(offer.id);
    this.interviewCv.set(this.cvText);
    await this.openInterviews();
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
    this.adminOfferPage.set(Math.min(this.adminOfferPage(), Math.max(0, Math.ceil(this.adminOffers().length / this.adminOfferPageSize) - 1)));
    this.jobOffers.set(this.adminOffers().filter(offer => offer.active));
  }

  showAnalyzer(): void { this.currentView.set('analyzer'); this.closeMobileSidebar(); }
  openCodeDebugger(): void { this.returnToWorkspace(); this.currentView.set('debug'); this.closeMobileSidebar(); }
  async analyzeCode(): Promise<void> {
    if (!this.debugCode().trim() || this.debugLoading()) return;
    this.debugLoading.set(true);
    this.debugError.set('');
    this.debugResult.set(null);
    try {
      const response = await this.authorizedFetch(`${this.apiUrl}/code-debug`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ module: this.debugModule(), language: this.debugLanguage(), code: this.debugCode(), expected_behavior: this.debugExpectedBehavior() }),
      });
      if (!response.ok) throw new Error(await this.responseError(response, 'Code analysis failed.'));
      this.debugResult.set(await response.json() as CodeDebugResult);
    } catch (error) {
      this.debugError.set(error instanceof Error ? error.message : 'Code analysis failed.');
    } finally {
      this.debugLoading.set(false);
    }
  }
  getOfferCategory(offer: JobOffer): string { return offer.description.match(/^Domain:\s*(.+)$/m)?.[1]?.trim() ?? 'Other'; }
  getOfferExcerpt(offer: JobOffer): string { return offer.description.split('\n').filter(line => !line.startsWith('Fictional CareerLens demo listing') && !line.startsWith('Domain: ') && !line.startsWith('Level: ')).join(' '); }
  setJobSearch(value: string): void { this.jobSearch.set(value); this.jobPage.set(0); }
  setJobCategory(value: string): void { this.jobCategory.set(value); this.jobPage.set(0); }
  changeJobPage(delta: number): void { this.jobPage.set(Math.min(this.jobPageCount() - 1, Math.max(0, this.jobPage() + delta))); }
  changeAdminOfferPage(delta: number): void { this.adminOfferPage.set(Math.min(this.adminOfferPageCount() - 1, Math.max(0, this.adminOfferPage() + delta))); }
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
