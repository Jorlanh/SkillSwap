import { NavLink } from '@/components/NavLink';
import {
  User, MessageSquare, FileText, Settings, Sun, BookOpen, Home, Users, Menu, X, Trophy, BriefcaseBusiness
} from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';

interface AppSidebarProps {
  isAuthenticated: boolean;
}

const sidebarLinks = [
  { to: '/', label: 'Início', icon: Home, public: true },
  { to: '/rankings', label: 'Rankings', icon: Trophy, public: true },
  { to: '/perfil', label: 'Perfil', icon: User, public: false },
  { to: '/mensagens', label: 'Mensagens', icon: MessageSquare, public: false },
  { to: '/propostas', label: 'Propostas', icon: FileText, public: false },
  { to: '/comunidades', label: 'Comunidades', icon: Users, public: true },
  { to: '/configuracoes', label: 'Configurações', icon: Settings, public: false },
  { to: '/preferencias', label: 'Preferências', icon: Sun, public: false },
  { to: '/cursos', label: 'Meus cursos', icon: BookOpen, public: false },
  { to: '/acesso', label: 'Minha jornada', icon: BriefcaseBusiness, public: false },
];

const AppSidebar = ({ isAuthenticated }: AppSidebarProps) => {
  const [mobileOpen, setMobileOpen] = useState(false);

  // Filtra os links com base na autenticação
  const visibleLinks = sidebarLinks.filter(link => link.public || isAuthenticated);
  const links = (closeAfterNavigate = false) => visibleLinks.map(({ to, label, icon: Icon }) => (
    <NavLink
      key={to}
      to={to}
      end={to === '/'}
      onClick={() => closeAfterNavigate && setMobileOpen(false)}
      className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-muted-foreground hover:bg-muted transition-colors"
      activeClassName="bg-muted text-foreground font-medium"
    >
      <Icon className="h-5 w-5" />
      <span>{label}</span>
    </NavLink>
  ));

  return (
    <>
      <Button
        variant="ghost"
        size="icon"
        className="fixed left-2 top-3 z-[80] h-10 w-10 rounded-lg border border-border bg-card shadow-sm lg:hidden"
        aria-label={mobileOpen ? 'Fechar menu de navegação' : 'Abrir menu de navegação'}
        onClick={() => setMobileOpen(!mobileOpen)}
      >
        {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </Button>

      {mobileOpen && (
        <div
          className="fixed inset-0 z-[60] bg-foreground/20 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside className="hidden lg:block sticky top-0 left-0 z-40 h-screen w-52 pt-6 px-2 bg-card border-r border-border">
        <nav className="flex flex-col gap-1">{links()}</nav>
      </aside>

      {mobileOpen && <aside className="fixed inset-y-0 left-0 z-[70] w-72 max-w-[85vw] overflow-y-auto bg-card border-r border-border pt-20 px-3 shadow-xl lg:hidden">
        <nav className="flex flex-col gap-1">{links(true)}</nav>
      </aside>}
    </>
  );
};

export default AppSidebar;
