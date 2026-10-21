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
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-card border-t border-border lg:hidden safe-area-bottom">
      <div className="flex items-center justify-around h-14">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className="flex flex-col items-center gap-0.5 px-3 py-1.5 text-muted-foreground transition-colors"
            activeClassName="text-primary"
          >
            <Icon className="h-5 w-5" />
            <span className="text-[10px] font-medium">{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
};

export default BottomNav;