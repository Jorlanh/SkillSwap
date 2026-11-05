import { Link, useNavigate } from 'react-router-dom';
import { Search, Home, Bell, LogIn, ShieldCheck } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { useEffect, useState } from 'react';
import { UserService } from '@/services/api';
import { auth } from '@/lib/firebase';
import { onIdTokenChanged, signOut } from 'firebase/auth';

const Header = () => {
  const [user, setUser] = useState<any>(null);
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const unsubscribe = onIdTokenChanged(auth, (firebaseUser) => {
      if (firebaseUser) {
      UserService.getMe() 
        .then(res => setUser(res.data))
        .catch(() => {
          console.warn("Sessão inválida no backend.");
          setUser(null);
        });
      } else {
        setUser(null);
      }
    });
    return unsubscribe;
  }, []);

  const handleLogin = () => {
    navigate('/login');
  };

  const handleLogout = () => {
    signOut(auth).finally(() => {
      setUser(null);
      navigate('/');
    });
  };

  return (
    <header className="sticky top-0 z-[70] w-full overflow-visible bg-card border-b border-border">
      <div className="relative flex min-h-16 w-full items-center gap-2 px-2 py-2 sm:px-6 sm:py-3">
        <Link to="/" className="flex min-w-0 shrink-0 items-center gap-2 pl-12 sm:pl-0 group transition-all active:scale-95">
          {/* 🚀 APENAS A LOGO S.PNG, SEM CÍRCULO VERDE */}
          <img 
            src="/skillswap-logo.png" 
            alt="SkillSwap" 
            className="h-7 w-auto max-w-[82px] object-contain transition-transform group-hover:scale-105 sm:h-10 sm:max-w-[180px]" 
          />
        </Link>

        {/* BUSCA CENTRALIZADA */}
        <div className="flex min-w-0 flex-1 justify-center mx-1 sm:absolute sm:left-1/2 sm:top-1/2 sm:w-[min(52vw,36rem)] sm:-translate-x-1/2 sm:-translate-y-1/2 sm:mx-0">
          <div className="relative w-full">
            <Input
              placeholder="Buscar mestre em..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && search.trim()) navigate(`/busca?query=${encodeURIComponent(search.trim())}`);
              }}
              className="h-9 pr-9 bg-muted border-border rounded-full focus-visible:ring-skillswap-teal sm:h-11"
            />
            <button type="button" aria-label="Pesquisar" onClick={() => search.trim() && navigate(`/busca?query=${encodeURIComponent(search.trim())}`)} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-skillswap-teal sm:right-3"><Search className="h-4 w-4" /></button>
          </div>
        </div>

        {/* NAVEGAÇÃO E PERFIL */}
        <div className="flex shrink-0 items-center gap-1.5 sm:gap-6">
          <Link to="/" title="Home" className="hidden sm:block text-foreground hover:text-skillswap-teal transition-all">
            <Home className="h-6 w-6" />
          </Link>
          <Link to="/atividades" title="Notificações" className="hidden sm:block text-foreground hover:text-skillswap-teal transition-all">
            <Bell className="h-6 w-6" />
          </Link>

          {user ? (
            <div className="flex items-center gap-1.5 sm:gap-4 animate-in fade-in slide-in-from-right-2">
              {user.roles?.some((role: string) => {
                const normalized = role.toUpperCase();
                return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN';
              }) && (
                <Link to="/admin" title="Painel do administrador" aria-label="Painel do administrador" className="text-red-500 hover:text-red-400 transition-colors">
                  <ShieldCheck className="h-5 w-5 sm:h-6 sm:w-6" />
                </Link>
              )}
              <Link to="/perfil">
                {/* Mantive o Teal apenas na borda do perfil para destacar o usuário logado */}
                <Avatar className="h-8 w-8 border-2 border-skillswap-teal hover:scale-105 transition-transform bg-muted/30 shadow-sm sm:h-10 sm:w-10 sm:border-[3px]">
                  <AvatarImage src={user.profile?.imageUrl || user.avatarUrl} />
                  <AvatarFallback className="font-bold text-lg text-foreground bg-transparent">
                    {user.username?.[0]?.toUpperCase() || 'U'}
                  </AvatarFallback>
                </Avatar>
              </Link>
              <button 
                onClick={handleLogout}
                className="hidden sm:block text-[11px] uppercase font-black text-skillswap-teal hover:text-destructive transition-colors tracking-widest"
              >
                SAIR
              </button>
            </div>
          ) : (
            <button 
              onClick={handleLogin}
                className="flex items-center gap-1.5 bg-skillswap-teal text-white px-3 py-2 rounded-full text-xs font-bold hover:bg-teal-700 shadow-lg transition-all active:scale-95 sm:gap-2 sm:px-6 sm:py-2.5 sm:text-sm"
            >
              <LogIn size={18} /> Entrar
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
