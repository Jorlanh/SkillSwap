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
    <header className="sticky top-0 z-50 bg-card border-b border-border">
      <div className="flex items-center justify-between px-6 py-3">
        <Link to="/" className="flex items-center gap-2 group transition-all active:scale-95">
          {/* 🚀 APENAS A LOGO S.PNG, SEM CÍRCULO VERDE */}
          <img 
            src="/skillswap-logo.png" 
            alt="SkillSwap" 
            className="h-10 w-auto max-w-[180px] object-contain transition-transform group-hover:scale-105" 
          />
        </Link>

        {/* BUSCA CENTRALIZADA */}
        <div className="flex-1 max-w-xl mx-8">
          <div className="relative">
            <Input
              placeholder="Buscar mestre em..."
              className="pr-10 bg-muted border-border rounded-full focus-visible:ring-skillswap-teal h-11"
            />
            <Search className="absolute right-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          </div>
        </div>

        {/* NAVEGAÇÃO E PERFIL */}
        <div className="flex items-center gap-6">
          <Link to="/" title="Home" className="text-foreground hover:text-skillswap-teal transition-all">
            <Home className="h-6 w-6" />
          </Link>
          <Link to="/atividades" title="Notificações" className="text-foreground hover:text-skillswap-teal transition-all">
            <Bell className="h-6 w-6" />
          </Link>

          {user ? (
            <div className="flex items-center gap-4 animate-in fade-in slide-in-from-right-2 ml-2">
              {user.roles?.some((role: string) => role.toUpperCase() === 'ADMIN') && (
                <Link to="/admin" title="Painel do administrador" className="text-red-500 hover:text-red-400 transition-colors">
                  <ShieldCheck className="h-6 w-6" />
                </Link>
              )}
              <Link to="/perfil">
                {/* Mantive o Teal apenas na borda do perfil para destacar o usuário logado */}
                <Avatar className="h-10 w-10 border-[3px] border-skillswap-teal hover:scale-105 transition-transform bg-muted/30 shadow-sm">
                  <AvatarImage src={user.profile?.imageUrl || user.avatarUrl} />
                  <AvatarFallback className="font-bold text-lg text-foreground bg-transparent">
                    {user.username?.[0]?.toUpperCase() || 'U'}
                  </AvatarFallback>
                </Avatar>
              </Link>
              <button 
                onClick={handleLogout}
                className="text-[11px] uppercase font-black text-skillswap-teal hover:text-destructive transition-colors tracking-widest"
              >
                SAIR
              </button>
            </div>
          ) : (
            <button 
              onClick={handleLogin}
              className="flex items-center gap-2 bg-skillswap-teal text-white px-6 py-2.5 rounded-full text-sm font-bold hover:bg-teal-700 shadow-lg transition-all active:scale-95 ml-2"
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
