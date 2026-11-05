import { NavLink } from '@/components/NavLink';
import { Home, Search, MessageSquare, User, Trophy, LogIn } from 'lucide-react';

interface BottomNavProps {
  isAuthenticated: boolean;
}

const BottomNav = ({ isAuthenticated }: BottomNavProps) => {
  const navItems = [
    { to: '/', label: 'Início', icon: Home },
    { to: '/busca', label: 'Busca', icon: Search },
    { to: '/rankings', label: 'Rankings', icon: Trophy },
    // Só mostra Chat e Perfil se logado, caso contrário mostra Login
    ...(isAuthenticated 
      ? [
          { to: '/mensagens', label: 'Chat', icon: MessageSquare },
          { to: '/perfil', label: 'Perfil', icon: User }
        ]
      : [
          { to: '/login', label: 'Entrar', icon: LogIn }
        ]
    )
  ];

  return (
    <nav className="mobile-bottom-nav fixed bottom-0 left-0 right-0 z-50 bg-card border-t border-border lg:hidden">
      <div className="flex h-16 w-full items-center justify-around gap-0.5 px-1">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className="flex min-w-0 flex-1 flex-col items-center gap-0.5 px-1 py-1.5 text-muted-foreground transition-colors"
            activeClassName="text-primary"
          >
            <Icon className="h-5 w-5" />
            <span className="max-w-full truncate text-[9px] font-medium sm:text-[10px]">{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
};

export default BottomNav;
