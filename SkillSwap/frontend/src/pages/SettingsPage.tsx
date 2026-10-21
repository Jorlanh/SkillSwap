import { useEffect, useState } from 'react';
import { mockAccessLogs } from '@/data/mockData';
import { Switch } from '@/components/ui/switch';
import { ChevronRight, Clock, Camera, Mic, MapPin, Users, Shield, Key, Lock, Download, Trash2, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { deleteUser } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { PrivacyService, UserService } from '@/services/api';
import { toast } from 'sonner';
import { requestNotificationPermission } from '@/lib/messaging';

type SettingsTab = 'historico' | 'permissoes' | 'seguranca' | 'privacidade';

const SettingsPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<SettingsTab>('historico');
  const [consents, setConsents] = useState<any[]>([]);
  const [minorStatus, setMinorStatus] = useState<any>(null);
  const [privacyForm, setPrivacyForm] = useState({ name: '', phoneNumber: '', bio: '', city: '', state: '', country: '', description: '', location: '' });

  const [permissions, setPermissions] = useState({
    camera: true,
    microfone: true,
    localizacao: false,
    contatos: false,
  });

  useEffect(() => {
    if (activeTab !== 'privacidade') return;
    Promise.all([PrivacyService.getConsents(), UserService.getMe(), PrivacyService.minorStatus()])
      .then(([consentResponse, profileResponse, minorResponse]) => {
        setConsents(consentResponse.data || []);
        setMinorStatus(minorResponse.data || null);
        const user = profileResponse.data || {};
        const profile = user.profile || {};
        setPrivacyForm((current) => ({ ...current, name: user.name || '', phoneNumber: user.phoneNumber || '', bio: user.bio || '', city: user.city || '', state: user.state || '', country: user.country || '', description: profile.description || '', location: profile.location || '' }));
      })
      .catch(() => toast.error('Não foi possível carregar o Centro de Privacidade.'));
  }, [activeTab]);

  const exportData = async () => {
    try {
      const response = await PrivacyService.exportData();
      const blob = new Blob([JSON.stringify(response.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'skillswap-meus-dados.json';
      link.click();
      URL.revokeObjectURL(url);
      toast.success('Exportação criada.');
    } catch { toast.error('Falha ao exportar os dados.'); }
  };

  const savePrivacyData = async () => {
    try {
      await PrivacyService.correctData(privacyForm);
      toast.success('Dados atualizados.');
    } catch { toast.error('Falha ao atualizar os dados.'); }
  };

  const revoke = async (purpose: string) => {
    try {
      await PrivacyService.revoke(purpose);
      setConsents((current) => [{ ...current[0], purpose, granted: false }, ...current]);
      toast.success('Consentimento opcional revogado.');
    } catch { toast.error('Falha ao revogar o consentimento.'); }
  };

  const grant = async (purpose: string) => {
    try {
      await PrivacyService.grant(purpose);
      if (purpose === 'OPTIONAL_NOTIFICATIONS') {
        const token = await requestNotificationPermission();
        if (token) await UserService.updateFcmToken(token);
      }
      setConsents((current) => [{ purpose, granted: true, recordedAt: new Date().toISOString() }, ...current]);
      toast.success('Consentimento opcional registrado.');
    } catch { toast.error('Falha ao registrar o consentimento.'); }
  };

  const deleteAccount = async () => {
    if (!window.confirm('A conta será desativada e os dados identificáveis serão removidos. Continuar?')) return;
    try {
      await PrivacyService.deleteAccount();
      if (auth.currentUser) await deleteUser(auth.currentUser).catch(() => undefined);
      localStorage.clear();
      navigate('/login');
    } catch { toast.error('Não foi possível concluir a exclusão da conta.'); }
  };

  return (
    <div className="max-w-3xl mx-auto">
      {/* Tab navigation */}
      <div className="flex gap-3 mb-6">
        {[
          { id: 'historico' as const, label: 'Histórico de Acesso' },
          { id: 'permissoes' as const, label: 'Permissões' },
          { id: 'seguranca' as const, label: 'Segurança' },
          { id: 'privacidade' as const, label: 'Privacidade' },
        ].map((tab) => (
          <button
            key={tab.id}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === tab.id
                ? 'bg-skillswap-light-blue text-foreground'
                : 'bg-muted text-muted-foreground hover:bg-accent'
            }`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="bg-skillswap-card-bg rounded-2xl p-6">
        {activeTab === 'historico' && (
          <>
            <h2 className="font-display font-bold text-xl text-foreground mb-4 flex items-center gap-2">
              <Clock className="h-5 w-5" /> Histórico de Acesso
            </h2>
            <div className="space-y-3">
              {mockAccessLogs.map((log) => (
                <div key={log.id} className="flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border">
                  <div>
                    <p className="text-sm font-medium text-foreground">{log.location}</p>
                    <p className="text-xs text-muted-foreground">{log.date}</p>
                  </div>
                  <span className="text-sm text-muted-foreground">ás {log.time}</span>
                </div>
              ))}
            </div>
          </>
        )}

        {activeTab === 'permissoes' && (
          <>
            <h2 className="font-display font-bold text-xl text-foreground mb-4 flex items-center gap-2">
              <Shield className="h-5 w-5" /> Permissões
            </h2>
            <h3 className="text-sm font-semibold text-foreground mb-3">Permitido</h3>
            <div className="space-y-2 mb-6">
              {[
                { key: 'camera' as const, label: 'Câmera', icon: Camera },
                { key: 'microfone' as const, label: 'Microfone', icon: Mic },
              ].map(({ key, label, icon: Icon }) => (
                <div key={key} className="flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border">
                  <div className="flex items-center gap-3">
                    <Icon className="h-5 w-5 text-foreground" />
                    <span className="text-sm font-medium text-foreground">{label}</span>
                  </div>
                  <Switch
                    checked={permissions[key]}
                    onCheckedChange={(v) => setPermissions((p) => ({ ...p, [key]: v }))}
                  />
                </div>
              ))}
            </div>
            <h3 className="text-sm font-semibold text-foreground mb-3">Não Permitido</h3>
            <div className="space-y-2">
              {[
                { key: 'localizacao' as const, label: 'Localização', icon: MapPin },
                { key: 'contatos' as const, label: 'Contatos', icon: Users },
              ].map(({ key, label, icon: Icon }) => (
                <div key={key} className="flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border">
                  <div className="flex items-center gap-3">
                    <Icon className="h-5 w-5 text-foreground" />
                    <span className="text-sm font-medium text-foreground">{label}</span>
                  </div>
                  <Switch
                    checked={permissions[key]}
                    onCheckedChange={(v) => setPermissions((p) => ({ ...p, [key]: v }))}
                  />
                </div>
              ))}
            </div>
          </>
        )}

        {activeTab === 'seguranca' && (
          <>
            <h2 className="font-display font-bold text-xl text-foreground mb-4 flex items-center gap-2">
              <Lock className="h-5 w-5" /> Segurança
            </h2>
            <div className="space-y-2">
              <button className="w-full flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border hover:opacity-90 transition-opacity">
                <div className="flex items-center gap-3">
                  <Key className="h-5 w-5 text-foreground" />
                  <span className="text-sm font-medium text-foreground">Alterar Senha</span>
                </div>
                <ChevronRight className="h-5 w-5 text-muted-foreground" />
              </button>
              <button className="w-full flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border hover:opacity-90 transition-opacity">
                <div className="flex items-center gap-3">
                  <Shield className="h-5 w-5 text-foreground" />
                  <span className="text-sm font-medium text-foreground">Autenticação em duas etapas</span>
                </div>
                <ChevronRight className="h-5 w-5 text-muted-foreground" />
              </button>
              <button className="w-full flex items-center justify-between bg-card rounded-xl px-4 py-3 border border-border hover:opacity-90 transition-opacity">
                <div className="flex items-center gap-3">
                  <Shield className="h-5 w-5 text-foreground" />
                  <span className="text-sm font-medium text-foreground">Permissões do App</span>
                </div>
                <ChevronRight className="h-5 w-5 text-muted-foreground" />
              </button>
            </div>
          </>
        )}

        {activeTab === 'privacidade' && (
          <div className="space-y-6">
            <div>
              <h2 className="font-display font-bold text-xl text-foreground mb-2 flex items-center gap-2"><Shield className="h-5 w-5" /> Centro de Privacidade</h2>
              <p className="text-sm text-muted-foreground">Consulte, corrija, exporte ou desative sua conta. A plataforma não vende seus dados.</p>
            </div>
            {minorStatus?.isMinor && (
              <div className="rounded-xl border border-skillswap-teal/30 bg-skillswap-teal/5 p-4" role="status">
                <h3 className="font-semibold">Confirmação de responsável</h3>
                <p className="mt-1 text-sm text-muted-foreground">Status da conta: <strong>{minorStatus.status}</strong>. Você continua podendo usar a plataforma enquanto a confirmação estiver pendente.</p>
              </div>
            )}
            <div className="flex flex-wrap gap-3">
              <a href="/privacidade" target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-lg border border-border px-4 py-2 text-sm hover:bg-muted"><FileText className="h-4 w-4" /> Política de Privacidade</a>
              <button onClick={exportData} className="inline-flex items-center gap-2 rounded-lg bg-skillswap-teal px-4 py-2 text-sm text-white"><Download className="h-4 w-4" /> Exportar meus dados</button>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {Object.entries(privacyForm).map(([key, value]) => (
                <label key={key} className="space-y-1 text-sm font-medium text-foreground">
                  <span>{({ name: 'Nome', phoneNumber: 'Telefone', bio: 'Biografia', city: 'Cidade', state: 'Estado', country: 'País', description: 'Descrição do perfil', location: 'Localização' } as Record<string, string>)[key]}</span>
                  {key === 'bio' || key === 'description' ? <textarea value={value} onChange={(e) => setPrivacyForm((current) => ({ ...current, [key]: e.target.value }))} className="min-h-24 w-full rounded-lg border border-border bg-background p-3" /> : <input value={value} onChange={(e) => setPrivacyForm((current) => ({ ...current, [key]: e.target.value }))} className="h-10 w-full rounded-lg border border-border bg-background px-3" />}
                </label>
              ))}
            </div>
            <button onClick={savePrivacyData} className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted">Salvar correções</button>
            <div className="rounded-xl border border-border p-4">
              <h3 className="font-semibold">Consentimentos opcionais</h3>
              <p className="mt-1 text-sm text-muted-foreground">Revogue integrações que não deseja utilizar. A revogação não apaga registros já necessários para segurança ou obrigações legais.</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {['OPTIONAL_AI', 'OPTIONAL_CALENDAR', 'OPTIONAL_VIDEO', 'OPTIONAL_NOTIFICATIONS'].map((purpose) => <span key={purpose} className="inline-flex gap-1"><button onClick={() => grant(purpose)} className="rounded-lg border border-border px-3 py-2 text-xs hover:bg-muted">Permitir {purpose.replace('OPTIONAL_', '')}</button><button onClick={() => revoke(purpose)} className="rounded-lg border border-border px-3 py-2 text-xs hover:bg-muted">Revogar</button></span>)}
              </div>
              <pre className="mt-4 max-h-36 overflow-auto rounded bg-muted p-3 text-xs">{JSON.stringify(consents, null, 2)}</pre>
            </div>
            <div className="rounded-xl border border-red-300 bg-red-50 p-4 dark:bg-red-950/20">
              <h3 className="font-semibold text-red-700 dark:text-red-300">Desativar e excluir minha conta</h3>
              <p className="mt-1 text-sm text-muted-foreground">O SkillSwap remove os dados identificáveis do perfil e tenta excluir a conta do Firebase.</p>
              <button onClick={deleteAccount} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm text-white"><Trash2 className="h-4 w-4" /> Desativar conta</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SettingsPage;
