import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Lock, LogIn, User, AtSign, Eye, EyeOff, X } from 'lucide-react'; 
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';

import { auth, googleProvider, githubProvider } from '@/lib/firebase';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signInWithPopup,
  updateProfile,
  deleteUser
} from 'firebase/auth';
import { PrivacyService, UserService } from '@/services/api'; 

interface AuthPageProps {
  isModal?: boolean;
  onClose?: () => void;
}

const AuthPage = ({ isModal, onClose }: AuthPageProps) => {
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [guardianName, setGuardianName] = useState('');
  const [guardianEmail, setGuardianEmail] = useState('');
  const [guardianRelationship, setGuardianRelationship] = useState('');
  const [acceptedRequiredPolicies, setAcceptedRequiredPolicies] = useState(false);

  const ageFromBirthDate = (value: string) => {
    if (!value) return null;
    const birth = new Date(`${value}T00:00:00`);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const birthdayNotReached = today.getMonth() < birth.getMonth()
      || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate());
    if (birthdayNotReached) age -= 1;
    return age;
  };
  const isMinorCandidate = (ageFromBirthDate(birthDate) ?? 18) < 18;

  const completeRegistrationCompliance = async () => {
    if (!birthDate || !acceptedRequiredPolicies) {
      throw new Error('Data de nascimento e aceite da política são obrigatórios.');
    }
    if (isMinorCandidate && (!guardianName.trim() || !guardianEmail.trim() || !guardianRelationship.trim())) {
      throw new Error('Para menores de 18 anos, informe os dados de um responsável.');
    }
    const ageGateResponse = await PrivacyService.ageGate(birthDate, isMinorCandidate ? {
      guardianName,
      guardianEmail,
      guardianRelationship,
      locale: 'pt-BR',
    } : undefined);
    await PrivacyService.recordConsent('PRIVACY_POLICY', true);
    await PrivacyService.recordConsent('TERMS_OF_SERVICE', true);
    return ageGateResponse.data;
  };

  const handleSuccess = async (user: any) => {
    try {
      await user.getIdToken(true);

      const profile = await UserService.getMe();
      const isAdmin = profile.data.roles?.some((role: string) => role.toUpperCase() === 'ADMIN');
      if (isAdmin) {
        const enterAdmin = window.confirm('Sua conta possui acesso administrativo.\n\nOK: entrar como administrador\nCancelar: continuar como usuário');
        localStorage.setItem('activeMode', enterAdmin ? 'admin' : 'user');
      } else {
        localStorage.setItem('activeMode', 'user');
      }

      toast.success(isLogin ? "Login efetuado com sucesso." : "Conta criada!");
      
      if (isModal && onClose) {
        onClose();
        window.location.reload(); // Atualiza o estado global de auth
      } else {
        navigate(localStorage.getItem('activeMode') === 'admin' ? '/admin' : '/');
      }
    } catch (error) {
      console.error("Erro no pós-login:", error);
      if (isModal && onClose) onClose();
      else navigate('/');
    }
  };

  const handleEmailAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isLogin) {
        const result = await signInWithEmailAndPassword(auth, email, password);
        await handleSuccess(result.user);
      } else {
        const result = await createUserWithEmailAndPassword(auth, email, password);
        await updateProfile(result.user, { displayName: username || fullName });
        const compliance = await completeRegistrationCompliance();
        if (compliance?.minor) {
          localStorage.setItem('guardianConsentStatus', compliance.guardianStatus || 'PENDING_GUARDIAN');
          toast.info('Conta criada. O responsável precisa confirmar o cadastro, mas você já pode acessar a plataforma.');
        }
        await handleSuccess(result.user);
      }
    } catch (error: any) {
      if (!isLogin && auth.currentUser) await deleteUser(auth.currentUser).catch(() => undefined);
      toast.error(error?.response?.data?.error || error?.message || "Falha na autenticação.");
    } finally {
      setLoading(false);
    }
  };

  const handleSocialAuth = async (providerName: string) => {
    setLoading(true);
    try {
      const provider = providerName === 'Google' ? googleProvider : githubProvider;
      const result = await signInWithPopup(auth, provider);
      if (!isLogin) {
        const compliance = await completeRegistrationCompliance();
        if (compliance?.minor) {
          localStorage.setItem('guardianConsentStatus', compliance.guardianStatus || 'PENDING_GUARDIAN');
          toast.info('Conta criada. O responsável precisa confirmar o cadastro, mas você já pode acessar a plataforma.');
        }
      }
      await handleSuccess(result.user);
    } catch (error) {
      if (!isLogin && auth.currentUser) await deleteUser(auth.currentUser).catch(() => undefined);
      toast.error((error as any)?.response?.data?.error || (error as any)?.message || `Falha ao conectar.`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`${isModal ? 'fixed inset-0 z-[100] flex items-center justify-center bg-background/80 backdrop-blur-sm p-4' : 'min-h-screen flex items-center justify-center bg-background p-4 relative overflow-hidden'}`}>
      {!isModal && (
        <>
          <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-skillswap-teal/10 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
          <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-skillswap-teal/10 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
        </>
      )}

      <div className="w-full max-w-md bg-card border border-border rounded-3xl shadow-2xl p-8 sm:p-10 z-10 relative animate-in fade-in zoom-in-95 duration-300">
        {isModal && (
          <button onClick={onClose} className="absolute right-6 top-6 text-muted-foreground hover:text-foreground"><X size={24}/></button>
        )}
        
        <div className="flex flex-col items-center mb-8">
          <img src="/skillswap-logo.png" alt="SkillSwap" className="w-auto h-32 mb-6 object-contain"/>
          <h1 className="text-3xl font-display font-bold text-foreground text-center">
            {isLogin ? 'Bem-vindo de volta' : 'Crie sua conta'}
          </h1>
        </div>

        <form onSubmit={handleEmailAuth} className="space-y-4">
          {!isLogin && (
            <div className="space-y-4 animate-in slide-in-from-top-2">
              <div className="relative group">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                <Input placeholder="Nome Completo" value={fullName} onChange={e => setFullName(e.target.value)} className="pl-10 h-12" />
              </div>
              <div className="relative group">
                <AtSign className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                <Input placeholder="Usuário" value={username} onChange={e => setUsername(e.target.value.toLowerCase())} className="pl-10 h-12" />
              </div>
              <div className="space-y-2">
                <label htmlFor="birthDate" className="text-sm font-medium text-foreground">Data de nascimento</label>
                <Input id="birthDate" type="date" value={birthDate} onChange={e => setBirthDate(e.target.value)} className="h-12" required />
                <p className="text-xs text-muted-foreground">Pessoas menores de 18 anos também podem usar o SkillSwap. Nesse caso, solicitamos a confirmação de um responsável por e-mail.</p>
              </div>
              {isMinorCandidate && (
                <div className="space-y-3 rounded-xl border border-skillswap-teal/30 bg-skillswap-teal/5 p-4" role="group" aria-labelledby="guardian-heading">
                  <div>
                    <h2 id="guardian-heading" className="text-sm font-semibold text-foreground">Confirmação de responsável</h2>
                    <p className="mt-1 text-xs text-muted-foreground">O responsável receberá um link seguro. A conta não será bloqueada enquanto a confirmação estiver pendente.</p>
                  </div>
                  <Input aria-label="Nome do responsável" placeholder="Nome do responsável" value={guardianName} onChange={e => setGuardianName(e.target.value)} required className="h-11" />
                  <Input aria-label="E-mail do responsável" type="email" placeholder="E-mail do responsável" value={guardianEmail} onChange={e => setGuardianEmail(e.target.value)} required className="h-11" />
                  <Input aria-label="Vínculo com o responsável" placeholder="Vínculo (ex.: mãe, pai, tutor)" value={guardianRelationship} onChange={e => setGuardianRelationship(e.target.value)} required className="h-11" />
                </div>
                )}
              <label className="flex items-start gap-3 text-sm text-muted-foreground">
                <input type="checkbox" checked={acceptedRequiredPolicies} onChange={e => setAcceptedRequiredPolicies(e.target.checked)} className="mt-1" required />
                <span>Li e aceito a <a href="/privacidade" target="_blank" rel="noreferrer" className="text-skillswap-teal underline">Política de Privacidade</a> e os Termos de Uso.</span>
              </label>
            </div>
          )}

          <div className="relative group">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input type="email" placeholder="E-mail" value={email} onChange={e => setEmail(e.target.value)} className="pl-10 h-12" />
          </div>

          <div className="relative group">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input type={showPassword ? "text" : "password"} placeholder="Senha" value={password} onChange={e => setPassword(e.target.value)} className="pl-10 pr-12 h-12" />
            <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"><Eye size={18}/></button>
          </div>

          <Button type="submit" disabled={loading} className="w-full bg-skillswap-teal hover:bg-teal-700 h-12 rounded-xl">
            {loading ? "Processando..." : isLogin ? "Acessar Conta" : 'Criar Conta'}
          </Button>
        </form>

        <div className="relative my-8 flex justify-center text-xs uppercase"><span className="bg-card px-4 text-muted-foreground font-semibold">Ou conecte-se com</span></div>

        <div className="flex justify-center gap-8">
          <button onClick={() => handleSocialAuth('Google')} className="w-16 h-16 rounded-full border border-border bg-card flex items-center justify-center hover:bg-muted transition-all active:scale-95 shadow-sm">
            <svg viewBox="0 0 24 24" className="w-8 h-8"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" /><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" /><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" /><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" /></svg>
          </button>
          <button onClick={() => handleSocialAuth('GitHub')} className="w-16 h-16 rounded-full border border-border bg-card flex items-center justify-center hover:bg-muted transition-all active:scale-95 shadow-sm">
            <svg viewBox="0 0 24 24" className="w-10 h-10 fill-foreground"><path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.43.372.823 1.102.823 2.222 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/></svg>
          </button>
        </div>

        <p className="mt-8 text-center text-sm text-muted-foreground">
          {isLogin ? "Ainda não faz parte? " : "Já tem conta? "}
          <button type="button" onClick={() => setIsLogin(!isLogin)} className="text-skillswap-teal font-bold hover:underline">
            {isLogin ? "Crie sua conta" : "Entre aqui"}
          </button>
        </p>
      </div>
    </div>
  );
};

export default AuthPage;
