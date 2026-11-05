import { useEffect, useState } from 'react';
import { Switch } from '@/components/ui/switch';
import { ChevronRight, Clock, Camera, Mic, MapPin, Users, Shield, Key, Lock } from 'lucide-react';
import { AccessLogService } from '@/services/api';
import { auth } from '@/lib/firebase';
import { onIdTokenChanged } from 'firebase/auth';
import { toast } from 'sonner';

type SettingsTab = 'historico' | 'permissoes' | 'seguranca';
type PermissionKey = 'camera' | 'microfone' | 'localizacao' | 'contatos';
type PermissionMap = Record<PermissionKey, boolean>;

const initialPermissions: PermissionMap = { camera: false, microfone: false, localizacao: false, contatos: false };

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('historico');
  const [accessLogs, setAccessLogs] = useState<any[]>([]);
  const [accessLogError, setAccessLogError] = useState(false);
  const [permissions, setPermissions] = useState<PermissionMap>(initialPermissions);

  useEffect(() => {
    let active = true;
    const unsubscribe = onIdTokenChanged(auth, (firebaseUser) => {
      if (!firebaseUser) {
        setAccessLogs([]);
        return;
      }
      setAccessLogError(false);
      AccessLogService.getMyAccessHistory()
        .then(({ data }) => { if (active) setAccessLogs(Array.isArray(data) ? data : []); })
        .catch(() => { if (active) setAccessLogError(true); });
    });
    return () => { active = false; unsubscribe(); };
  }, []);

  useEffect(() => {
    let active = true;
    const readPermission = async (name: 'camera' | 'microphone' | 'geolocation') => {
      if (!navigator.permissions?.query) return false;
      try {
        const result = await navigator.permissions.query({ name: name as PermissionName });
        return result.state === 'granted';
      } catch {
        return false;
      }
    };
    Promise.all([readPermission('camera'), readPermission('microphone'), readPermission('geolocation')]).then(([camera, microfone, localizacao]) => {
      if (active) setPermissions((current) => ({ ...current, camera, microfone, localizacao }));
    });
    return () => { active = false; };
  }, []);

  const requestMediaPermission = async (key: 'camera' | 'microfone') => {
    if (!navigator.mediaDevices?.getUserMedia) {
      toast.error('Este navegador não permite acessar câmera e microfone.');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: key === 'camera', audio: key === 'microfone' });
      stream.getTracks().forEach((track) => track.stop());
      setPermissions((current) => ({ ...current, [key]: true }));
      toast.success(`${key === 'camera' ? 'Câmera' : 'Microfone'} autorizado neste navegador.`);
    } catch {
      setPermissions((current) => ({ ...current, [key]: false }));
      toast.error(`Não foi possível liberar ${key === 'camera' ? 'a câmera' : 'o microfone'}. Verifique o cadeado ao lado do endereço do site.`);
    }
  };

  const requestLocationPermission = () => {
    if (!navigator.geolocation) {
      toast.error('Este navegador não oferece localização.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPermissions((current) => ({ ...current, localizacao: true }));
        AccessLogService.updateMyLocation(coords.latitude, coords.longitude)
          .then(() => AccessLogService.getMyAccessHistory())
          .then(({ data }) => setAccessLogs(Array.isArray(data) ? data : []))
          .catch(() => undefined);
        toast.success('Localização autorizada neste navegador e registrada no último acesso.');
      },
      () => { setPermissions((current) => ({ ...current, localizacao: false })); toast.error('A localização não foi autorizada. Revise o bloqueio nas configurações do navegador.'); },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 0 },
    );
  };

  const handlePermissionChange = (key: PermissionKey, value: boolean) => {
    if (!value) {
      setPermissions((current) => ({ ...current, [key]: false }));
      toast.info('Para revogar a permissão de verdade, use o cadeado ao lado do endereço do site.');
      return;
    }
    if (key === 'camera' || key === 'microfone') return void requestMediaPermission(key);
    if (key === 'localizacao') return requestLocationPermission();
    toast.info('O navegador não oferece acesso automático aos contatos.');
  };

  const permissionRows: Array<{ key: PermissionKey; label: string; icon: typeof Camera; note?: string; disabled?: boolean }> = [
    { key: 'camera', label: 'Câmera', icon: Camera },
    { key: 'microfone', label: 'Microfone', icon: Mic },
    { key: 'localizacao', label: 'Localização', icon: MapPin },
    { key: 'contatos', label: 'Contatos', icon: Users, note: 'Não disponível como permissão geral nos navegadores.', disabled: true },
  ];

  return (
    <main className="max-w-3xl mx-auto p-4 pb-20">
      <div className="flex flex-wrap gap-3 mb-6">
        {[
          { id: 'historico' as const, label: 'Histórico de Acesso' },
          { id: 'permissoes' as const, label: 'Permissões' },
          { id: 'seguranca' as const, label: 'Segurança' },
        ].map((tab) => (
          <button key={tab.id} type="button" className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${activeTab === tab.id ? 'bg-skillswap-light-blue text-foreground' : 'bg-muted text-muted-foreground hover:bg-accent'}`} onClick={() => setActiveTab(tab.id)}>
            {tab.label}
          </button>
        ))}
      </div>

      <div className="bg-skillswap-card-bg rounded-2xl p-4 sm:p-6">
        {activeTab === 'historico' && (
          <section aria-labelledby="access-history-title">
            <h1 id="access-history-title" className="font-display font-bold text-xl text-foreground mb-4 flex items-center gap-2"><Clock className="h-5 w-5" /> Histórico de Acesso</h1>
            <div className="space-y-3">
              {accessLogError ? <p className="text-sm text-destructive">Não foi possível carregar o histórico agora.</p> : accessLogs.length === 0 ? <p className="text-sm text-muted-foreground">Nenhum acesso registrado.</p> : accessLogs.map((log, index) => {
                const date = log.accessTime ? new Date(log.accessTime) : null;
                return <div key={`${log.accessTime ?? 'access'}-${index}`} className="flex flex-wrap items-center justify-between gap-2 bg-card rounded-xl px-4 py-3 border border-border"><div><p className="text-sm font-medium text-foreground">{log.location || log.city || 'Local não informado'}</p><p className="text-xs text-muted-foreground">{date ? date.toLocaleDateString('pt-BR') : 'Data não informada'}</p></div><span className="text-sm text-muted-foreground">{date ? `às ${date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}` : ''}</span></div>;
              })}
            </div>
          </section>
        )}

        {activeTab === 'permissoes' && (
          <section aria-labelledby="permissions-title">
            <h1 id="permissions-title" className="font-display font-bold text-xl text-foreground mb-2 flex items-center gap-2"><Shield className="h-5 w-5" /> Permissões do navegador</h1>
            <p className="text-sm text-muted-foreground mb-5">Ao ativar câmera, microfone ou localização, o navegador exibirá o pedido real de permissão. A SkillSwap não consegue revogar uma permissão por você.</p>
            <div className="space-y-3">
              {permissionRows.map(({ key, label, icon: Icon, note, disabled }) => (
                <div key={key} className="flex items-center justify-between gap-4 bg-card rounded-xl px-4 py-3 border border-border">
                  <div className="flex items-center gap-3 min-w-0"><Icon className="h-5 w-5 text-foreground shrink-0" /><span className="min-w-0"><span className="text-sm font-medium text-foreground block">{label}: {permissions[key] ? 'permitido' : 'não permitido'}</span>{note && <span className="text-xs text-muted-foreground block">{note}</span>}</span></div>
                  <Switch checked={permissions[key]} disabled={disabled} onCheckedChange={(value) => handlePermissionChange(key, value)} aria-label={`${label}: ${permissions[key] ? 'permitido' : 'não permitido'}`} />
                </div>
              ))}
            </div>
          </section>
        )}

        {activeTab === 'seguranca' && (
          <section aria-labelledby="security-title">
            <h1 id="security-title" className="font-display font-bold text-xl text-foreground mb-4 flex items-center gap-2"><Lock className="h-5 w-5" /> Segurança</h1>
            <div className="space-y-2">
              {[{ label: 'Alterar Senha', icon: Key }, { label: 'Autenticação em duas etapas', icon: Shield }, { label: 'Permissões do App', icon: Shield }].map(({ label, icon: Icon }) => (
                <button key={label} type="button" className="w-full flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border hover:opacity-90 transition-opacity"><div className="flex items-center gap-3"><Icon className="h-5 w-5 text-foreground" /><span className="text-sm font-medium text-foreground">{label}</span></div><ChevronRight className="h-5 w-5 text-muted-foreground" /></button>
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  );
};

export default SettingsPage;
