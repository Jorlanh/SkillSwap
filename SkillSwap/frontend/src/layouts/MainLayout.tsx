import { Outlet } from 'react-router-dom';
import Header from '@/components/Header';
import AppSidebar from '@/components/AppSidebar';
import BottomNav from '@/components/BottomNav';
import { AccessibilityToolbar } from '@/components/AccessibilityToolbar';
import { LibrasWidget } from '@/components/LibrasWidget';
import { auth } from '@/lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';
import { useEffect, useState } from 'react';
import { UserService } from '@/services/api';

interface MainLayoutProps {
  requireAuth: (action: () => void) => void;
}

const MainLayout = ({ requireAuth }: MainLayoutProps) => {
  const [isAuthenticated, setIsAuthenticated] = useState(Boolean(auth.currentUser));

  useEffect(() => onAuthStateChanged(auth, (user) => setIsAuthenticated(Boolean(user))), []);

  useEffect(() => {
    if (!isAuthenticated) return;
    void UserService.getMe().then(({ data }) => {
      const highContrast = ['high-contrast', 'high-contrast-dark', 'high-contrast-light'].includes(data?.preferredTheme);
      localStorage.setItem('highContrast', String(highContrast));
      if (data?.accessibilityVisualMarker) {
        localStorage.setItem('accessibilityVisualMarker', data.accessibilityVisualMarker);
      }
      document.documentElement.classList.toggle('high-contrast', highContrast);
      window.dispatchEvent(new CustomEvent('skillswap:accessibility-changed'));
    }).catch(() => undefined);
  }, [isAuthenticated]);

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="flex">
        {/* Agora o AppSidebar aceita a prop e o erro 2322 some! */}
        <AppSidebar isAuthenticated={isAuthenticated} />
        <main className="flex-1 min-h-[calc(100vh-57px)] p-4 lg:p-6 pb-20 lg:pb-6">
          {/* O Outlet passa o contexto para os filhos (PostCard, etc) */}
          <Outlet context={{ requireAuth }} />
        </main>
      </div>
      <BottomNav isAuthenticated={isAuthenticated} />
      <AccessibilityToolbar />
      <LibrasWidget />
    </div>
  );
};

export default MainLayout;
