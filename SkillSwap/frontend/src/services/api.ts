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
  try {
    // Aguarda o Firebase terminar de restaurar a sessão antes de decidir se
    // esta chamada precisa de autenticação.
    const firebaseAuth = auth as typeof auth & { authStateReady?: () => Promise<void> };
    if (firebaseAuth.authStateReady) await firebaseAuth.authStateReady();
    const token = auth.currentUser ? await auth.currentUser.getIdToken() : null;
    if (token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch {
    // Chamadas públicas continuam funcionando mesmo quando não há sessão.
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
  getActiveBans: () => api.get('/console-management/bans'),
  toggleVerification: (userId: string) => api.post(`/console-management/users/${userId}/toggle-verification`),
  banUser: (userId: string, data: any) => api.post(`/console-management/users/${userId}/ban`, data),
  unbanUser: (userId: string) => api.post(`/console-management/users/${userId}/unban`),
  setRole: (userId: string, role: string, enabled: boolean) =>
    api.put(`/console-management/users/${userId}/role`, { role, enabled }),
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
  getTrending: () => api.get('/home/trending', { params: { period: 'DAY' } }),
  getTrendingTopics: (period: string = 'DAY') => api.get('/home/trending-topics', { params: { period } }),
  getTrendingPosts: (period: string = 'DAY') => api.get('/home/trending-posts', { params: { period } }),
  getPosts: (sortBy: string = 'TRENDING', period: string = 'DAY') => api.get('/home/posts', { params: { sortBy, period } }),
  createPost: (formData: FormData) => api.post('/home/posts', formData),
  likePost: (postId: number) => api.post(`/home/posts/${postId}/like`),
  repost: (postId: number) => api.post(`/home/posts/${postId}/repost`),
  comment: (postId: number, content: string, parentCommentId?: number) => api.post(`/home/posts/${postId}/comment`, null, { params: { content, ...(parentCommentId ? { parentCommentId } : {}) } }),
  getCommunities: (limit: number = 5) => api.get('/home/communities', { params: { limit } }),
  createCommunity: (data: { name: string; description?: string; privateCommunity?: boolean }) => api.post('/home/communities', data),
  joinCommunity: (communityId: string) => api.post(`/home/communities/${communityId}/join`),
  getUnreadNotifications: () => api.get('/home/notifications'),
  markNotificationRead: (notificationId: number) => api.post(`/home/notifications/${notificationId}/read`),
  generateShareLink: (postId: number) => api.get(`/home/posts/${postId}/share`),
  trackShareClick: (shareUrl: string) => api.post('/home/share/click', null, { params: { shareUrl } }),
  getPostLikes: (postId: number) => api.get(`/home/posts/${postId}/likes`),
  getPostComments: (postId: number) => api.get(`/home/posts/${postId}/comments`),
  likeComment: (commentId: number) => api.post(`/home/comments/${commentId}/like`),
  deleteComment: (commentId: number) => api.delete(`/home/comments/${commentId}`),
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
  getById: (communityId: string) => api.get(`/communities/${communityId}`),
  join: (communityId: string) => api.post(`/communities/${communityId}/join`),
  getMembership: (communityId: string) => api.get(`/communities/${communityId}/membership`),
  getMembers: (communityId: string, search = '') => api.get(`/communities/${communityId}/members`, { params: { search } }),
  getPendingMembers: (communityId: string, search = '') => api.get(`/communities/${communityId}/members/pending`, { params: { search } }),
  removeMember: (communityId: string, memberId: string) => api.delete(`/communities/${communityId}/members/${memberId}`),
  setAdministrator: (communityId: string, memberId: string, administrator: boolean) => api.put(`/communities/${communityId}/members/${memberId}/admin`, null, { params: { administrator } }),
  approveMember: (communityId: string, memberId: string) => api.put(`/communities/${communityId}/members/${memberId}/approve`),
  updateAvatar: (communityId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/communities/${communityId}/avatar`, formData);
  },
  getPosts: (communityId: string) => api.get(`/communities/${communityId}/posts`),
  createPost: (communityId: string, formData: FormData) => api.post(`/communities/${communityId}/posts`, formData),
  likePost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/like`),
  commentPost: (communityId: string, postId: number, commentData: any) => api.post(`/communities/${communityId}/posts/${postId}/comment`, commentData),
  repostPost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/repost`),
  sharePost: (communityId: string, postId: number) => api.post(`/communities/${communityId}/posts/${postId}/share`),
};

export const StoryService = {
  getAll: () => api.get('/stories'),
  create: (formData: FormData) => api.post('/stories', formData),
};

// ==========================================
// USUÁRIO & PERFIL RESTAURADO
// ==========================================
export const ProfileService = {
  get: (handle: string) => api.get(`/profiles/user/${handle}`),
  update: (data: any) => api.put('/profiles/me', data),
  togglePrivacy: (isPrivate: boolean) => api.patch('/profiles/me/privacy', null, { params: { isPrivate } }),
  updateAvatar: (formData: FormData) => api.post('/profiles/me/avatar', formData),
  removeAvatar: () => api.delete('/profiles/me/avatar'),
  getMyFeed: () => api.get('/profiles/me/feed'),
  getMyCommunityPosts: () => api.get('/profiles/me/community-posts'),
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
  confirmCompletion: (lessonId: number) => api.patch(`/lesson/${lessonId}/complete`),
  getCertificate: (lessonId: number) => api.get(`/lesson/${lessonId}/certificate`),
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
export const AccessService = {
  getOverview: () => api.get('/access/overview'),
  getRecommendations: () => api.get('/access/recommendations'),
  getOpportunities: () => api.get('/access/opportunities'),
  createOpportunity: (data: any) => api.post('/access/opportunities', data),
  deleteOpportunity: (opportunityId: string) => api.delete(`/access/opportunities/${opportunityId}`),
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
  updateAccessibility: (data: any) => api.patch('/settings/accessibility/update', data),
  getAccessibilityPreferences: () => api.get('/accessibility/preferences'),
  updateAccessibilityPreferences: (data: any) => api.put('/accessibility/preferences', data),
};

export const ReportService = {
  create: (data: any) => api.post('/reports', data),
};

export default api;
