import { useEffect, useState } from 'react';
import { Eye, Keyboard, Languages, Save, Volume2, Type } from 'lucide-react';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { SettingsService } from '@/services/api';
import { toast } from 'sonner';
import { auth } from '@/lib/firebase';
import { onIdTokenChanged } from 'firebase/auth';

type AccessibilityPreferences = {
  contrastMode: 'default' | 'high-contrast';
  fontScale: 'small' | 'medium' | 'large';
  preferredChannel: 'text' | 'audio' | 'video' | 'any';
  captionsEnabled: boolean;
  imageDescriptionsEnabled: boolean;
  simplifiedLanguageEnabled: boolean;
  librasEnabled: boolean;
  keyboardNavigationEnabled: boolean;
  visualMarker: 'standard' | 'focus' | 'contrast';
  accessibilitySymbol: 'blue-triangle' | 'orange-square' | 'white-circle' | null;
};

const defaults: AccessibilityPreferences = {
  contrastMode: 'default',
  fontScale: 'medium',
  preferredChannel: 'any',
  captionsEnabled: false,
  imageDescriptionsEnabled: true,
  simplifiedLanguageEnabled: true,
  librasEnabled: false,
  keyboardNavigationEnabled: true,
  visualMarker: 'standard',
  accessibilitySymbol: null,
};

const PreferencesPage = () => {
  const [preferences, setPreferences] = useState<AccessibilityPreferences>(defaults);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const setup = new URLSearchParams(window.location.search).get('setup') === '1';

  const applyLocally = (next: AccessibilityPreferences) => {
    const root = document.documentElement;
    if (next.contrastMode === 'high-contrast') root.dataset.contrast = 'high';
    else delete root.dataset.contrast;
    root.dataset.fontScale = next.fontScale;
    localStorage.setItem('skillswap-accessibility-preferences', JSON.stringify(next));
  };

  useEffect(() => {
    let active = true;
    const loadLocal = () => {
      const stored = localStorage.getItem('skillswap-accessibility-preferences');
      if (stored) {
        try {
          const next = { ...defaults, ...JSON.parse(stored) } as AccessibilityPreferences;
          if (active) setPreferences(next);
          applyLocally(next);
          return;
        } catch { /* usa os valores padrão abaixo */ }
      }
      if (active) setPreferences(defaults);
      applyLocally(defaults);
    };
    const load = async (authenticated: boolean) => {
      if (!authenticated) {
        loadLocal();
        if (active) setLoading(false);
        return;
      }
      try {
        const response = await SettingsService.getAccessibilityPreferences();
        const next = { ...defaults, ...response.data } as AccessibilityPreferences;
        if (active) setPreferences(next);
        applyLocally(next);
      } catch {
        loadLocal();
      } finally {
        if (active) setLoading(false);
      }
    };
    const unsubscribe = onIdTokenChanged(auth, (firebaseUser) => { void load(Boolean(firebaseUser)); });
    return () => { active = false; unsubscribe(); };
  }, []);

  const update = <K extends keyof AccessibilityPreferences>(key: K, value: AccessibilityPreferences[K]) => {
    const next = { ...preferences, [key]: value };
    setPreferences(next);
    applyLocally(next);
  };

  const save = async () => {
    if (!auth.currentUser) {
      toast.error('Entre na sua conta para salvar as preferências no perfil.');
      return;
    }
    if (setup && !preferences.accessibilitySymbol) {
      toast.error('Escolha uma opção para concluir a triagem de acessibilidade.');
      return;
    }
    setSaving(true);
    try {
      const response = await SettingsService.updateAccessibilityPreferences(preferences);
      const next = { ...defaults, ...response.data } as AccessibilityPreferences;
      setPreferences(next);
      applyLocally(next);
      toast.success('Preferências de acessibilidade salvas.');
    } catch {
      toast.error('Não foi possível salvar agora. As escolhas continuam ativas neste dispositivo.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="max-w-3xl mx-auto p-6 text-muted-foreground">Carregando preferências...</div>;

  return (
    <main className="max-w-3xl mx-auto p-4 pb-20">
      <Card className="p-6 md:p-8 space-y-8">
        <header>
          <p className="text-sm font-semibold text-skillswap-teal">{setup ? 'Primeiro acesso' : 'Configurações'}</p>
          <h1 className="text-3xl font-display font-bold text-foreground mt-1">Preferências de acessibilidade</h1>
          <p className="text-muted-foreground mt-3 leading-6">
            {setup
              ? 'Escolha como prefere usar a SkillSwap. Você pode mudar essas opções quando quiser.'
              : 'Estas escolhas ajustam a interface e não servem para diagnosticar ou classificar ninguém.'}
          </p>
        </header>

        <section aria-labelledby="accessibility-symbol-title" className="space-y-4 rounded-xl border border-skillswap-teal/30 bg-skillswap-light-blue/40 p-4">
          <div>
            <h2 id="accessibility-symbol-title" className="font-bold text-xl text-foreground">Como você prefere usar a plataforma?</h2>
            <p className="text-sm text-muted-foreground mt-2 leading-6">
              Esta escolha é feita por você para orientar o atendimento e a apresentação do seu perfil. Ela não diagnostica deficiência, não substitui uma avaliação profissional e pode ser alterada depois.
            </p>
          </div>
          <div className="grid gap-3" role="radiogroup" aria-labelledby="accessibility-symbol-title">
            {([
              ['blue-triangle', 'Azul com triângulo', 'Quero indicar uma necessidade de acessibilidade e receber uma experiência mais cuidadosa.'],
              ['orange-square', 'Laranja com quadrado', 'Quero indicar que preciso de apoio prático em algum recurso da plataforma.'],
              ['white-circle', 'Branco com círculo', 'Não quero indicar uma necessidade específica neste momento.'],
            ] as const).map(([value, label, description]) => (
              <button
                type="button"
                key={value}
                role="radio"
                aria-checked={preferences.accessibilitySymbol === value}
                onClick={() => update('accessibilitySymbol', value)}
                className={`flex items-start gap-3 rounded-lg border p-3 text-left transition-colors ${preferences.accessibilitySymbol === value ? 'border-skillswap-teal ring-2 ring-skillswap-teal bg-background' : 'border-border bg-background/70 hover:bg-background'}`}
              >
                <span aria-hidden="true" className={`mt-0.5 h-8 w-8 shrink-0 ${value === 'blue-triangle' ? 'bg-sky-500 [clip-path:polygon(50%_0,100%_100%,0_100%)]' : value === 'orange-square' ? 'bg-orange-500 rounded-sm' : 'bg-white border border-slate-400 rounded-full'}`} />
                <span>
                  <span className="font-semibold block text-foreground">{label}</span>
                  <span className="text-sm text-muted-foreground">{description}</span>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section aria-labelledby="visual-title" className="space-y-4">
          <h2 id="visual-title" className="font-bold text-xl flex items-center gap-2"><Eye size={20} /> Visual</h2>
          <div className="grid md:grid-cols-2 gap-4">
            <label className="space-y-2">
              <span className="text-sm font-semibold">Contraste</span>
              <select value={preferences.contrastMode} onChange={(e) => update('contrastMode', e.target.value as AccessibilityPreferences['contrastMode'])} className="w-full h-11 rounded-lg border border-input bg-background px-3">
                <option value="default">Padrão</option>
                <option value="high-contrast">Contraste intenso</option>
              </select>
            </label>
            <label className="space-y-2">
              <span className="text-sm font-semibold flex items-center gap-2"><Type size={16} /> Tamanho do texto</span>
              <select value={preferences.fontScale} onChange={(e) => update('fontScale', e.target.value as AccessibilityPreferences['fontScale'])} className="w-full h-11 rounded-lg border border-input bg-background px-3">
                <option value="small">Pequeno</option>
                <option value="medium">Médio</option>
                <option value="large">Grande</option>
              </select>
            </label>
          </div>
          <div className="grid md:grid-cols-3 gap-3" role="group" aria-label="Marcador visual decorativo">
            {([
              ['standard', 'Padrão', 'Sem marcador adicional'],
              ['focus', 'Foco', 'Realça elementos ativos'],
              ['contrast', 'Contraste', 'Realça bordas e áreas importantes'],
            ] as const).map(([value, label, description]) => (
              <button type="button" key={value} onClick={() => update('visualMarker', value)} aria-pressed={preferences.visualMarker === value} className={`rounded-lg border p-3 text-left ${preferences.visualMarker === value ? 'border-skillswap-teal ring-2 ring-skillswap-teal' : 'border-border'}`}>
                <span className="font-semibold block">{label}</span>
                <span className="text-xs text-muted-foreground">{description}</span>
              </button>
            ))}
          </div>
        </section>

        <section aria-labelledby="communication-title" className="space-y-4">
          <h2 id="communication-title" className="font-bold text-xl flex items-center gap-2"><Volume2 size={20} /> Comunicação</h2>
          <label className="space-y-2 block">
            <span className="text-sm font-semibold">Canal preferido</span>
            <select value={preferences.preferredChannel} onChange={(e) => update('preferredChannel', e.target.value as AccessibilityPreferences['preferredChannel'])} className="w-full h-11 rounded-lg border border-input bg-background px-3">
              <option value="any">Qualquer canal</option>
              <option value="text">Texto</option>
              <option value="audio">Áudio</option>
              <option value="video">Vídeo</option>
            </select>
          </label>
          <div className="grid gap-3">
            <div className="flex items-center justify-between gap-4 border rounded-lg p-3">
              <span><strong>Legendas quando disponíveis</strong><small className="block text-muted-foreground">Recurso complementar; a disponibilidade depende da sala e do navegador.</small></span>
              <Switch checked={preferences.captionsEnabled} onCheckedChange={(value) => update('captionsEnabled', value)} aria-label="Ativar legendas quando disponíveis" />
            </div>
            <div className="flex items-center justify-between gap-4 border rounded-lg p-3">
              <span><strong>Descrição de imagens</strong><small className="block text-muted-foreground">Mantém descrições textuais junto às imagens relevantes.</small></span>
              <Switch checked={preferences.imageDescriptionsEnabled} onCheckedChange={(value) => update('imageDescriptionsEnabled', value)} aria-label="Ativar descrição de imagens" />
            </div>
            <div className="flex items-center justify-between gap-4 border rounded-lg p-3">
              <span><strong>VLibras</strong><small className="block text-muted-foreground">Tradução assistida opcional; não substitui intérprete ou texto.</small></span>
              <Switch checked={preferences.librasEnabled} onCheckedChange={(value) => update('librasEnabled', value)} aria-label="Ativar VLibras" />
            </div>
          </div>
        </section>

        <section aria-labelledby="operation-title" className="space-y-4">
          <h2 id="operation-title" className="font-bold text-xl flex items-center gap-2"><Keyboard size={20} /> Operação</h2>
          <div className="flex items-center justify-between gap-4 border rounded-lg p-3">
            <span><strong>Navegação por teclado</strong><small className="block text-muted-foreground">Mantém foco visível e alternativa ao uso do mouse.</small></span>
            <Switch checked={preferences.keyboardNavigationEnabled} onCheckedChange={(value) => update('keyboardNavigationEnabled', value)} aria-label="Ativar navegação por teclado" />
          </div>
          <div className="flex items-center justify-between gap-4 border rounded-lg p-3">
            <span><strong>Linguagem simplificada</strong><small className="block text-muted-foreground">Prefere instruções curtas e mensagens diretas.</small></span>
            <Switch checked={preferences.simplifiedLanguageEnabled} onCheckedChange={(value) => update('simplifiedLanguageEnabled', value)} aria-label="Ativar linguagem simplificada" />
          </div>
          <p className="text-xs text-muted-foreground">Os marcadores de cor e forma desta tela são apenas escolhas visuais. Eles não representam deficiência, diagnóstico ou condição de saúde.</p>
        </section>

        <Button type="button" onClick={save} disabled={saving} className="w-full h-12 bg-skillswap-teal hover:bg-teal-700 text-white">
          <Save size={18} className="mr-2" /> {saving ? 'Salvando...' : 'Salvar preferências'}
        </Button>
      </Card>
    </main>
  );
};

export default PreferencesPage;
