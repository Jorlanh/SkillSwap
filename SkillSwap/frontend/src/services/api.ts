import axios from 'axios';
import { auth } from '@/lib/firebase';

// Utiliza a variável do .env com fallback para localhost caso falhe
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  // CORREÇÃO CRÍTICA: O Content-Type global não deve existir para que FormData funcione
});

// Interceptor para injetar o Token JWT em cada requisição
api.interceptors.request.use(async (config) => {
  const token = auth.currentUser ? await auth.currentUser.getIdToken() : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ==========================================
// AUTENTICAÇÃO
// ==========================================
export const AuthService = {
  login: (credentials: any) => api.post('/auth/login', credentials),
  logout: () => api.post('/settings/logout'),
};

// ==========================================
// IA ELOS
// ==========================================
export const ChatbotService = {
  ask: (message: string, simplify: boolean) => 
    api.post('/chatbot/ask', { message, simplifyResponse: simplify }),
};

// ==========================================
// ADMINISTRAÇÃO
// ==========================================
export const AdminService = {
  getStats: () => api.get('/console-management/stats'),
  getOperationsReport: () => api.get('/console-management/operations-report'),
  getLedgerAudit: (page = 0, size = 50) => api.get('/console-management/ledger', { params: { page, size } }),
  adjustWallet: (userId: string, amount: number, reason: string, idempotencyKey?: string) =>
    api.post(`/console-management/users/${userId}/wallet-adjustment`, { amount, reason, idempotencyKey }),
  getUsers: () => api.get('/console-management/users'),
  toggleVerification: (userId: string) => api.post(`/console-management/users/${userId}/toggle-verification`),
  banUser: (userId: string, data: any) => api.post(`/console-management/users/${userId}/ban`, data),
  unbanUser: (userId: string) => api.post(`/console-management/users/${userId}/unban`),
  setRole: (userId: string, role: string, enabled: boolean) =>
    api.put(`/console-management/users/${userId}/role`, { role, enabled }),
  getModerationCases: () => api.get('/console-management/moderation/cases'),
  getModerationPolicy: () => api.get('/console-management/moderation/policy'),
  updateModerationPolicy: (data: { automaticTemporaryBanEnabled: boolean; automaticTemporaryBanHours: number }) =>
    api.put('/console-management/moderation/policy', data),
  decideModeration: (reportId: number, data: {
    action: 'RELEASE' | 'WARNING' | 'SUSPEND_DAYS' | 'SUSPEND_MONTHS' | 'PERMANENT_BAN';
    reason: string;
    evidence?: string;
    durationDays?: number;
    humanConfirmation: boolean;
  }) => api.post(`/console-management/moderation/cases/${reportId}/decision`, data),
};

// ==========================================
// ROLES E SKILLS
// ==========================================
export const RoleService = {
  getAll: () => api.get('/roles'),
  getById: (id: number) => api.get(`/roles/${id}`),
  create: (data: any) => api.post('/roles', data),
  update: (id: number, data: any) => api.put(`/roles/${id}`, data),
  delete: (id: number) => api.delete(`/roles/${id}`),
};

export const SkillService = {
  getAll: () => api.get('/skills'),
  getById: (id: number) => api.get(`/skills/${id}`),
  create: (data: any) => api.post('/skills', data),
  update: (id: number, data: any) => api.put(`/skills/${id}`, data),
  delete: (id: number) => api.delete(`/skills/${id}`),
};

// ==========================================
// HOME E FEED RESTAURADO
// ==========================================
export const HomeService = {
  getFeed: () => api.get('/home/feed'),
  getTrending: () => api.get('/home/trending-topics'), // Restaurado para evitar erro no FeedPage.tsx
  getTrendingTopics: (period: string = 'DAY') => api.get('/home/trending-topics', { params: { period } }),
  getTrendingPosts: (period: string = 'DAY') => api.get('/home/trending-posts', { params: { period } }),
  getPosts: (sortBy: string = 'TRENDING', period: string = 'DAY') => api.get('/home/posts', { params: { sortBy, period } }),
  createPost: (formData: FormData) => api.post('/home/posts', formData),
  likePost: (postId: number) => api.post(`/home/posts/${postId}/like`),
  repost: (postId: number) => api.post(`/home/posts/${postId}/repost`),
  comment: (postId: number, content: string) => api.post(`/home/posts/${postId}/comment`, null, { params: { content } }),
  getCommunities: (limit: number = 5) => api.get('/home/communities', { params: { limit } }),
  joinCommunity: (communityId: string) => api.post(`/home/communities/${communityId}/join`),
  getUnreadNotifications: () => api.get('/home/notifications'),
  markNotificationRead: (notificationId: number) => api.post(`/home/notifications/${notificationId}/read`),
  generateShareLink: (postId: number) => api.get(`/home/posts/${postId}/share`),
  trackShareClick: (shareUrl: string) => api.post('/home/share/click', null, { params: { shareUrl } }),
  getPostLikes: (postId: number) => api.get(`/home/posts/${postId}/likes`),
  getPostComments: (postId: number) => api.get(`/home/posts/${postId}/comments`),
};

// ==========================================
// POSTS (RESTAURADO PARA COMPATIBILIDADE COM O PostCard.tsx)
// ==========================================
export const PostService = {
  getAll: () => api.get('/posts'),
  getById: (id: string | number) => api.get(`/posts/${id}`),
  create: (formData: FormData) => api.post('/posts', formData),
  like: (id: string | number) => api.post(`/posts/${id}/like`),
  comment: (id: string | number, content: string) => api.post(`/posts/${id}/comment`, null, { params: { content } }),
  repost: (id: string | number) => api.post(`/posts/${id}/repost`),
  delete: (id: string | number) => api.delete(`/posts/${id}`),
};

// ==========================================
// BUSCA & COMUNIDADES
// ==========================================
export const SearchService = {
  search: (query: string, filter: string = 'ALL', sortBy: string = 'RELEVANCE') => 
    api.get('/search', { params: { query, filter, sortBy } }),
};

export const CommunityService = {
  getAll: () => api.get('/communities'), // RESTAURADO para resolver o erro no ActivitiesPage
  join: (communityId: string) => api.post(`/communities/${communityId}/join`),
  getPosts: (communityId: string) => api.get(`/communities/${communityId}/posts`),
  createPost: (communityId: string, formData: FormData) => api.post(`/communities/${communityId}/posts`, formData),
  likePost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/like`),
  commentPost: (communityId: string, postId: number, commentData: any) => api.post(`/communities/${communityId}/posts/${postId}/comment`, commentData),
  repostPost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/repost`),
  sharePost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/share`),
};

// ==========================================
// USUÁRIO & PERFIL RESTAURADO
// ==========================================
export const ProfileService = {
  get: (handle: string) => api.get(`/profiles/user/${handle}`),
  update: (data: any) => api.put('/profiles/me', data),
  togglePrivacy: (isPrivate: boolean) => api.patch('/profiles/me/privacy', null, { params: { isPrivate } }),
  updateAvatar: (formData: FormData) => api.post('/profiles/me/avatar', formData),
  getMyFeed: () => api.get('/profiles/me/feed'),
  createMyPost: (formData: FormData) => api.post('/profiles/me/posts', formData),
  deleteMyPost: (postId: number) => api.delete(`/profiles/me/posts/${postId}`),
  likeMyPost: (postId: number) => api.post(`/profiles/me/posts/${postId}/like`),
  commentMyPost: (postId: number, content: string) => api.post(`/profiles/me/posts/${postId}/comment`, null, { params: { content } }),
  followUser: (targetUserId: string) => api.post(`/profiles/me/follow/${targetUserId}`),
  unfollowUser: (targetUserId: string) => api.post(`/profiles/me/unfollow/${targetUserId}`),
};

export const UserService = {
  getAll: () => api.get('/users'),
  getById: (id: string) => api.get(`/users/${id}`),
  getMe: () => api.get('/users/me'),
  getByUsername: (username: string) => api.get(`/users/${username}`),
  update: (id: string, data: any) => api.put(`/users/${id}`, data),
  delete: (id: string) => api.delete(`/users/${id}`),

  // RESTAURADO PARA O AUTHPAGE FUNCIONAR (updateFcmToken)
  updateFcmToken: (token: string) => api.post('/devices/fcm-token', { token }),

  updateProfile: (profileData: any) => ProfileService.update(profileData),
  updateAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return ProfileService.updateAvatar(formData);
  },
  togglePrivacy: (isPrivate: boolean) => ProfileService.togglePrivacy(isPrivate),
  followUser: (targetUserId: string) => ProfileService.followUser(targetUserId),
  unfollowUser: (targetUserId: string) => ProfileService.unfollowUser(targetUserId),
};

// ==========================================
// MENSAGERIA (CHAT)
// ==========================================
export const ChatService = {
  getConversations: () => api.get('/chat/conversations'),
  sendMessage: (payload: any) => api.post('/chat/send', payload),
  sendVoiceMessage: (formData: FormData) => api.post('/chat/send-voice', formData),
  uploadAttachment: (formData: FormData) => api.post('/chat/upload-file', formData),
  getHistory: (userId1: string, userId2: string) => api.get(`/chat/history/${userId1}/${userId2}`),
};

// ==========================================
// CURSOS (LESSONS)
// ==========================================
export const LessonService = {
  schedule: (data: any) => api.post('/lesson/schedule', data),
  getUpcoming: (userId: string | number) => api.get(`/lesson/upcoming/${userId}`),
  getMine: () => api.get('/lesson/mine'),
  complete: (lessonId: number) => api.post(`/lesson/${lessonId}/complete`),
  getCertificate: (lessonId: number) => api.get(`/lesson/certificate/${lessonId}`),
  notify: (lessonId: number) => api.post(`/lesson/notify/${lessonId}`),
};

// ==========================================
// NOTIFICAÇÕES
// ==========================================
export const NotificationService = {
  getAll: () => api.get('/notifications'),
  markRead: (id: string | number) => api.put(`/notifications/${id}/read`),
};

// ==========================================
// PROPOSTAS (RESTURADO COM DUALIDADE PARA NÃO QUEBRAR)
// ==========================================
export const ProposalService = {
  // Rotas que chamam o ProposalController do Java
  send: (data: any) => api.post('/proposal/send', data),
  getUserProposals: (userId: string) => api.get(`/proposal/user/${userId}`),
  accept: (proposalId: number) => api.post(`/proposal/accept/${proposalId}`),
  negotiate: (proposalId: number) => api.post(`/proposal/negotiate/${proposalId}`),
  reject: (proposalId: number) => api.post(`/proposal/reject/${proposalId}`),
  block: (proposalId: number) => api.post(`/proposal/block/${proposalId}`),
  complete: (proposalId: number) => api.post(`/proposal/complete/${proposalId}`),
  
  // RESTAURADO O QUE O FRONTEND ESTAVA USANDO ANTERIORMENTE PARA NÃO QUEBRAR
  create: (data: any) => api.post('/proposal/send', data),
  getAll: (userId?: string) => userId ? api.get(`/proposal/user/${userId}`) : api.get('/proposal/user/me'), // Ajuste flexivel
};

// ==========================================
// VÍDEO
// ==========================================
export const VideoService = {
  getJoinToken: (roomName: string) => api.post('/video/join-room', { roomName }),
};

// ==========================================
// RANKINGS RESTAURADO COMPLETO (RankingService)
// ==========================================
export const RatingService = {
  submit: (data: any) => api.post('/ratings', data),
};

export const RankingService = {
  getRankings: (skillId?: number) => api.get('/rankings', { params: { skillId } }),
  getAll: () => api.get('/rankings'), // Restaurado para RankingsPage.tsx
  getBySkill: (skillId: string | number) => api.get('/rankings', { params: { skillId } }), // Restaurado
  getSkills: () => api.get('/skills'), // Restaurado (Embora exista no SkillService, estava chamando daqui)
};

// ==========================================
// CONQUISTAS
// ==========================================
export const AchievementService = {
  getAll: () => api.get('/achievements'),
  generateImage: (data: any) => api.post('/achievements/generate-image', data),
  getUserAchievements: (userId: string | number) => api.get(`/achievements/user/${userId}`),
};

// ==========================================
// LOGS DE ACESSO
// ==========================================
export const AccessLogService = {
  getMyAccessHistory: () => api.get('/access-logs/me'),
};

// ==========================================
// PERMISSÕES
// ==========================================
export const PermissionService = {
  toggle: (permissionType: string) => api.post(`/permissions/toggle/${permissionType}`),
  choose: (permissionType: string, choice: string) => api.post(`/permissions/choose/${permissionType}`, null, { params: { choice } }),
};

// ==========================================
// FINANCEIRO & CONFIGURAÇÕES & REPORTES
// ==========================================
export const FinanceService = {
  getWallet: () => api.get('/wallet'),
  getPackages: () => api.get('/wallet/packages'),
  createCheckoutSession: (packageCode: string) => api.post('/wallet/checkout-sessions', { packageCode }),
  getHistory: (page = 0, size = 20) => api.get('/wallet/ledger', { params: { page, size } }),
  transfer: (recipientUserId: string, amount: number, note?: string) =>
    api.post('/wallet/transfers', { recipientUserId, amount, note }),
  tip: (recipientUserId: string) => api.post('/wallet/tips', { recipientUserId, amount: 5 }),
};

export const AccessService = {
  getOverview: () => api.get('/access/overview'),
  getRecommendations: () => api.get('/access/recommendations'),
  getOpportunities: () => api.get('/access/opportunities'),
  createOpportunity: (data: any) => api.post('/access/opportunities', data),
};

// ==========================================
// WORKSPACE INSTITUCIONAL B2B2C
// ==========================================
export const InstitutionalService = {
  getCatalog: () => api.get('/institutional/catalog'),
  getOrganizations: () => api.get('/institutional/organizations'),
  createOrganization: (data: unknown) => api.post('/institutional/organizations', data),
  getDashboard: (organizationId: string) => api.get(`/institutional/organizations/${organizationId}/dashboard`),
  getMembers: (organizationId: string) => api.get(`/institutional/organizations/${organizationId}/members`),
  addMember: (organizationId: string, data: unknown) => api.post(`/institutional/organizations/${organizationId}/members`, data),
  updateMember: (organizationId: string, membershipId: string, data: unknown) => api.patch(`/institutional/organizations/${organizationId}/members/${membershipId}`, data),
  removeMember: (organizationId: string, membershipId: string) => api.delete(`/institutional/organizations/${organizationId}/members/${membershipId}`),
  getOpportunities: (organizationId: string) => api.get(`/institutional/organizations/${organizationId}/opportunities`),
  createOpportunity: (organizationId: string, data: unknown) => api.post(`/institutional/organizations/${organizationId}/opportunities`, data),
  updateOpportunityStatus: (organizationId: string, opportunityId: string, status: string) => api.patch(`/institutional/organizations/${organizationId}/opportunities/${opportunityId}/status`, { status }),
  getApplications: (organizationId: string) => api.get(`/institutional/organizations/${organizationId}/applications`),
  updateApplicationStatus: (organizationId: string, applicationId: string, status: string) => api.patch(`/institutional/organizations/${organizationId}/applications/${applicationId}/status`, { status }),
  getSubscription: (organizationId: string) => api.get(`/institutional/organizations/${organizationId}/subscription`),
  updateSubscription: (organizationId: string, data: unknown) => api.put(`/institutional/organizations/${organizationId}/subscription`, data),
  createBillingCheckout: (organizationId: string, data: unknown) => api.post(`/institutional/organizations/${organizationId}/billing/checkout`, data),
};

// ==========================================
// JORNADA DE OPORTUNIDADES
// Plano -> prática -> evidência -> candidatura
// ==========================================
export const OpportunityOsService = {
  getDashboard: () => api.get('/opportunity-os/dashboard'),
  createPlan: (data: any) => api.post('/opportunity-os/plan', data),
  updateStep: (stepId: number, status: string) => api.patch(`/opportunity-os/plan/steps/${stepId}`, { status }),
  getEvidence: () => api.get('/opportunity-os/evidence'),
  createEvidence: (data: any) => api.post('/opportunity-os/evidence', data),
  requestEvidenceReview: (evidenceId: string) => api.post(`/opportunity-os/evidence/${evidenceId}/review`),
  getApplications: () => api.get('/opportunity-os/applications'),
  apply: (opportunityId: string, message?: string) => api.post(`/opportunity-os/opportunities/${opportunityId}/apply`, { message }),
};

export const PremiumService = {
  getCatalog: () => api.get('/premium/catalog'),
  getAiUsage: () => api.get('/premium/usage/ai'),
  profilePlan: () => api.post('/premium/ai/profile-plan'),
  boostProposal: (proposalId: number) => api.post(`/premium/proposals/${proposalId}/boost`),
  highlightPost: (postId: number) => api.post(`/premium/posts/${postId}/highlight`),
  sponsorProposal: (proposalId: number) => api.post(`/premium/proposals/${proposalId}/sponsor`),
  sponsorPost: (postId: number) => api.post(`/premium/posts/${postId}/sponsor`),
  addPortfolioSlot: () => api.post('/premium/profile/portfolio-slots'),
  addBioLimit: () => api.post('/premium/profile/bio-limit'),
  addPostLimit: () => api.post('/premium/profile/post-limit'),
  buyExtraLessonHours: (lessonId: number, hours: number) =>
    api.post(`/premium/lessons/${lessonId}/extra-hours`, { hours }),
};

export const KarmaService = {
  getMe: () => api.get('/karma/me'),
  getTop: () => api.get('/karma/top'),
};

export const SettingsService = {
  getAccessLogs: () => api.get('/settings/access-logs'),
  getPermissions: () => api.get('/settings/permissions'),
  updatePermission: (key: string, value: boolean) => api.put('/settings/permissions', { key, value }),
  getPreferences: () => api.get('/settings/preferences'),
  updatePreferences: (data: any) => api.put('/settings/preferences', data),
  changePassword: (data: any) => api.put('/settings/security/password', data),
  updateAccessibility: (data: {
    preferredTheme?: string;
    accessibilityVisualMarker?: string;
    librasAvatarEnabled?: boolean;
    accessibilityVisualMarkerPublic?: boolean;
  }) => api.patch('/settings/accessibility/update', data),
};

export const PrivacyService = {
  getPolicy: () => api.get('/privacy/policy'),
  getMinorPolicy: () => api.get('/privacy/minor-policy'),
  ageGate: (birthDate: string, guardian?: { guardianName?: string; guardianEmail?: string; guardianRelationship?: string; locale?: string }) =>
    api.post('/privacy/me/age-gate', { birthDate, ...guardian }),
  minorStatus: () => api.get('/privacy/me/minor-status'),
  guardianConsentStatus: (token: string) => api.get('/privacy/guardian-consent', { params: { token } }),
  confirmGuardianConsent: (data: { token: string; guardianName: string; relationship: string; consentGranted: boolean }) =>
    api.post('/privacy/guardian-consent/confirm', data),
  getConsents: () => api.get('/privacy/me/consents'),
  recordConsent: (purpose: string, granted = true, policyVersion?: string, locale = 'pt-BR') =>
    api.post('/privacy/me/consents', { purpose, granted, policyVersion, locale }),
  grant: (purpose: string) => api.post('/privacy/me/consents', { purpose, granted: true, locale: 'pt-BR' }),
  revoke: (purpose: string) => api.post(`/privacy/me/consents/${purpose}/revoke`),
  exportData: () => api.get('/privacy/me/export'),
  correctData: (data: Record<string, unknown>) => api.patch('/privacy/me', data),
  deleteAccount: () => api.delete('/privacy/me'),
};

export const ReportService = {
  create: (data: any) => api.post('/reports', data),
  appeal: (reportId: number, reason: string) => api.post(`/reports/${reportId}/appeal`, { reason }),
};

export default api;
