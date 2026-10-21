import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Toaster as Sonner } from '@/components/ui/sonner';
import { Toaster } from '@/components/ui/toaster';
import { TooltipProvider } from '@/components/ui/tooltip';
import MainLayout from '@/layouts/MainLayout';
import FeedPage from '@/pages/FeedPage';
import ChatPage from '@/pages/ChatPage';
import ProfilePage from '@/pages/ProfilePage';
import SearchPage from '@/pages/SearchPage';
import CommunitiesPage from '@/pages/CommunitiesPage';
import FinancePage from '@/pages/FinancePage';
import ActivitiesPage from '@/pages/ActivitiesPage';
import ProposalsPage from '@/pages/ProposalsPage';
import PreferencesPage from '@/pages/PreferencesPage';
import SettingsPage from '@/pages/SettingsPage';
import CoursesPage from '@/pages/CoursesPage';
import RankingsPage from '@/pages/RankingsPage';
import NotFound from './pages/NotFound';
import AdminConsole from '@/pages/AdminConsole';
import OpportunityOsPage from '@/pages/OpportunityOsPage';
import InstitutionalWorkspacePage from '@/pages/InstitutionalWorkspacePage';
import PrivacyPage from '@/pages/PrivacyPage';
import { ChatbotELOS } from '@/components/ChatbotELOS';
import AuthPage from '@/pages/AuthPage';
import GuardianConsentPage from '@/pages/GuardianConsentPage';
import { onMessageListener } from './lib/messaging';
import { auth } from './lib/firebase';
import { onIdTokenChanged } from 'firebase/auth';

const queryClient = new QueryClient();

const App = () => {
  const [showAuthModal, setShowAuthModal] = useState(false);
  // A sessão é mantida pelo Firebase Auth. Tokens de acesso nunca são
  // persistidos no localStorage; o interceptor solicita um token fresco
  // diretamente ao SDK quando uma chamada protegida é feita.
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => onIdTokenChanged(auth, async (user) => {
    if (user) {
      setIsAuthenticated(true);
      onMessageListener();
    } else {
      setIsAuthenticated(false);
    }
  }), []);

  const requireAuth = (action: () => void) => {
    if (!isAuthenticated) setShowAuthModal(true);
    else action();
  };

  const ProtectedRoute = ({ children }: { children: ReactNode }) => (
    isAuthenticated ? <>{children}</> : <AuthPage />
  );

  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Sonner position="top-right" richColors />
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          {showAuthModal && <AuthPage isModal onClose={() => setShowAuthModal(false)} />}
          <Routes>
            <Route path="/login" element={<AuthPage />} />
            <Route path="/privacidade" element={<PrivacyPage />} />
            <Route path="/consentimento-responsavel" element={<GuardianConsentPage />} />
            <Route element={<MainLayout requireAuth={requireAuth} />}>
              <Route path="/" element={<FeedPage />} />
              <Route path="/mensagens" element={<ChatPage />} />
              <Route path="/perfil/:username?" element={<ProfilePage />} />
              <Route path="/busca" element={<SearchPage />} />
              <Route path="/comunidades" element={<CommunitiesPage />} />
              <Route path="/financeiro" element={<FinancePage />} />
              <Route path="/atividades" element={<ActivitiesPage />} />
              <Route path="/propostas" element={<ProposalsPage />} />
              <Route path="/preferencias" element={<PreferencesPage />} />
              <Route path="/configuracoes" element={<SettingsPage />} />
              <Route path="/cursos" element={<CoursesPage />} />
              <Route path="/rankings" element={<RankingsPage />} />
              <Route path="/uniaosinistrabahvas" element={<AdminConsole />} />
              <Route path="/admin" element={<AdminConsole />} />
              <Route path="/acesso" element={<ProtectedRoute><OpportunityOsPage /></ProtectedRoute>} />
              <Route path="/jornada" element={<ProtectedRoute><OpportunityOsPage /></ProtectedRoute>} />
              <Route path="/institucional" element={<ProtectedRoute><InstitutionalWorkspacePage /></ProtectedRoute>} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Routes>
          {isAuthenticated && <ChatbotELOS />}
        </BrowserRouter>
      </TooltipProvider>
    </QueryClientProvider>
  );
};

export default App;
