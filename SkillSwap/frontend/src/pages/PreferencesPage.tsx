import { useState, useEffect } from 'react';
import { Switch } from '@/components/ui/switch';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Moon, Type, Palette, Eye, Check, RotateCcw, Monitor } from 'lucide-react';
import { toast } from 'sonner';
import { SettingsService, UserService } from '@/services/api';
import VisualMarker, { VISUAL_MARKERS, normalizeVisualMarker, type VisualMarkerKind } from '@/components/VisualMarker';

// 🛡️ CONSTANTES PADRÃO DA SKILLSWAP
const DEFAULTS = {
  theme: 'light',
  customTheme: 'default',
  fontSize: 'medium',
  fontFamily: 'Inter',
  colorBlindMode: 'none',
  brandColor: '#0d9488', // Teal Original
  bgColor: '#ffffff',
  navColor: '#ffffff',
  textColor: '#000000'
};

const PreferencesPage = () => {
  // Estados carregando do LocalStorage com Fallback para os DEFAULTS
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark');
  const [customTheme, setCustomTheme] = useState(() => localStorage.getItem('customTheme') || DEFAULTS.customTheme);
  const [fontSize, setFontSize] = useState(() => localStorage.getItem('fontSize') || DEFAULTS.fontSize);
  const [fontFamily, setFontFamily] = useState(() => localStorage.getItem('fontFamily') || DEFAULTS.fontFamily);
  const [colorBlindMode, setColorBlindMode] = useState(() => localStorage.getItem('colorBlindMode') || DEFAULTS.colorBlindMode);
  const [highContrast, setHighContrast] = useState(() => localStorage.getItem('highContrast') === 'true');
  const [visualMarker, setVisualMarker] = useState<VisualMarkerKind>(() => normalizeVisualMarker(localStorage.getItem('accessibilityVisualMarker')));
  const [visualMarkerPublic, setVisualMarkerPublic] = useState(false);

  // Cores Customizadas
  const [brandColor, setBrandColor] = useState(() => localStorage.getItem('brandColor') || DEFAULTS.brandColor);
  const [bgColor, setBgColor] = useState(() => localStorage.getItem('bgColor') || DEFAULTS.bgColor);
  const [navColor, setNavColor] = useState(() => localStorage.getItem('navColor') || DEFAULTS.navColor);
  const [textColor, setTextColor] = useState(() => localStorage.getItem('textColor') || DEFAULTS.textColor);

  const availableFonts = [
    'Inter', 'Roboto', 'Montserrat', 'Open Sans', 'Poppins', 'Playfair Display', 'Oswald', 'Ubuntu', 'JetBrains Mono'
  ];

  // 🔄 FUNÇÃO DE RESET GLOBAL
  const handleResetToDefaults = () => {
    setDarkMode(false);
    setCustomTheme(DEFAULTS.customTheme);
    setFontSize(DEFAULTS.fontSize);
    setFontFamily(DEFAULTS.fontFamily);
    setColorBlindMode(DEFAULTS.colorBlindMode);
    setHighContrast(false);
    setVisualMarker('NEUTRAL_CIRCLE');
    setBrandColor(DEFAULTS.brandColor);
    setBgColor(DEFAULTS.bgColor);
    setNavColor(DEFAULTS.navColor);
    setTextColor(DEFAULTS.textColor);
    
    toast.info("Configurações resetadas para o padrão SkillSwap.");
  };

  useEffect(() => {
    UserService.getMe().then(({ data }) => {
      setVisualMarkerPublic(Boolean(data?.accessibilityVisualMarkerPublic));
    }).catch(() => undefined);
  }, []);

  useEffect(() => {
    const root = document.documentElement;

    // 1. APLICAÇÃO DO MODO ESCURO
    if (darkMode) {
      root.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      root.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }

    root.classList.toggle('high-contrast', highContrast);
    root.dataset.visualMarker = visualMarker;
    localStorage.setItem('highContrast', String(highContrast));
    localStorage.setItem('accessibilityVisualMarker', visualMarker);

    // 2. TEMAS E CORES GLOBAIS
    localStorage.setItem('customTheme', customTheme);
    if (highContrast) {
      // O alto contraste tem prioridade sobre cores personalizadas para manter
      // a legibilidade e o foco visível em toda a interface.
      ['--background', '--card', '--primary', '--skillswap-teal', '--foreground'].forEach(prop => root.style.removeProperty(prop));
    } else if (customTheme === 'cr7') {
      root.style.setProperty('--background', '222 47% 11%');
      root.style.setProperty('--card', '222 47% 15%');
      root.style.setProperty('--primary', '45 93% 47%');
      root.style.setProperty('--skillswap-teal', '#cda434');
      root.style.setProperty('--foreground', '0 0% 100%');
    } else if (customTheme === 'custom') {
      root.style.setProperty('--skillswap-teal', brandColor);
      root.style.setProperty('--background', hexToHsl(bgColor));
      root.style.setProperty('--card', hexToHsl(navColor));
      root.style.setProperty('--foreground', hexToHsl(textColor));
      localStorage.setItem('brandColor', brandColor);
      localStorage.setItem('bgColor', bgColor);
      localStorage.setItem('navColor', navColor);
      localStorage.setItem('textColor', textColor);
    } else {
      // Limpeza de propriedades para voltar ao CSS padrão
      ['--background', '--card', '--primary', '--skillswap-teal', '--foreground'].forEach(prop => root.style.removeProperty(prop));
    }

    // 3. TIPOGRAFIA GLOBAL
    localStorage.setItem('fontSize', fontSize);
    localStorage.setItem('fontFamily', fontFamily);
    root.style.fontSize = fontSize === 'small' ? '14px' : fontSize === 'large' ? '18px' : '16px';
    
    const linkId = 'dynamic-google-font';
    let link = document.getElementById(linkId) as HTMLLinkElement;
    if (!link) {
      link = document.createElement('link');
      link.id = linkId;
      link.rel = 'stylesheet';
      document.head.appendChild(link);
    }
    link.href = `https://fonts.googleapis.com/css2?family=${fontFamily.replace(' ', '+')}:wght@400;700&display=swap`;
    root.style.fontFamily = `'${fontFamily}', sans-serif`;

    // 4. ACESSIBILIDADE
    localStorage.setItem('colorBlindMode', colorBlindMode);
    root.style.filter = colorBlindMode !== 'none' ? `url(#${colorBlindMode}-filter)` : 'none';

  }, [darkMode, customTheme, fontSize, fontFamily, colorBlindMode, brandColor, bgColor, navColor, textColor, highContrast, visualMarker]);

  useEffect(() => {
    const syncFromToolbar = () => setHighContrast(localStorage.getItem('highContrast') === 'true');
    window.addEventListener('skillswap:accessibility-changed', syncFromToolbar);
    return () => window.removeEventListener('skillswap:accessibility-changed', syncFromToolbar);
  }, []);

  const saveAccessibilityPreferences = async () => {
    try {
      await SettingsService.updateAccessibility({
        preferredTheme: highContrast ? 'high-contrast' : 'default',
        accessibilityVisualMarker: visualMarker,
        accessibilityVisualMarkerPublic: visualMarkerPublic,
      });
      toast.success('Preferências de acessibilidade sincronizadas com sua conta.');
    } catch {
      toast.info('Preferência aplicada neste dispositivo. A sincronização com a conta ficará disponível quando o servidor estiver conectado.');
    }
  };

  function hexToHsl(hex: string) {
    let r = 0, g = 0, b = 0;
    if (hex.length === 4) { r = parseInt(hex[1] + hex[1], 16); g = parseInt(hex[2] + hex[2], 16); b = parseInt(hex[3] + hex[3], 16); }
    else { r = parseInt(hex.substring(1, 3), 16); g = parseInt(hex.substring(3, 5), 16); b = parseInt(hex.substring(5, 7), 16); }
    r /= 255; g /= 255; b /= 255;
    let max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h = 0, s, l = (max + min) / 2;
    if (max === min) { h = s = 0; }
    else {
      let d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      switch (max) {
        case r: h = (g - b) / d + (g < b ? 6 : 0); break;
        case g: h = (b - r) / d + 2; break;
        case b: h = (r - g) / d + 4; break;
      }
      h /= 6;
    }
    return `${Math.round(h * 360)} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`;
  }

  return (
    <div className="max-w-5xl mx-auto pb-20 animate-in fade-in duration-500 p-4">
      {/* Filtros de Daltonismo (SVG) */}
      <svg className="hidden">
        <defs>
          <filter id="protanopia-filter"><feColorMatrix type="matrix" values="0.567, 0.433, 0, 0, 0 0.558, 0.442, 0, 0, 0 0, 0.242, 0.758, 0, 0 0, 0, 0, 1, 0" /></filter>
          <filter id="deuteranopia-filter"><feColorMatrix type="matrix" values="0.625, 0.375, 0, 0, 0 0.7, 0.3, 0, 0, 0 0, 0.3, 0.7, 0, 0 0, 0, 0, 1, 0" /></filter>
          <filter id="tritanopia-filter"><feColorMatrix type="matrix" values="0.95, 0.05, 0, 0, 0 0, 0.433, 0.567, 0, 0 0, 0.475, 0.525, 0, 0 0, 0, 0, 1, 0" /></filter>
        </defs>
      </svg>

      <div className="flex flex-col md:flex-row justify-between items-center mb-10 gap-4">
        <div className="text-left">
          <h1 className="text-4xl font-display font-black text-foreground tracking-tight">Preferências Globais</h1>
          <p className="text-muted-foreground mt-2">Ajuste o ecossistema da SkillSwap ao seu estilo.</p>
        </div>
        <Button 
          variant="outline" 
          onClick={handleResetToDefaults}
          className="rounded-full flex gap-2 border-destructive text-destructive hover:bg-destructive hover:text-white transition-all font-bold"
        >
          <RotateCcw size={16} /> Restaurar Padrão
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* COLUNA 1: CORES E TEMAS */}
        <Card className="lg:col-span-2 p-8 border-border bg-card shadow-xl rounded-[32px] space-y-8">
          <div className="flex items-center gap-3 border-b border-border pb-4">
            <Palette className="text-skillswap-teal h-7 w-7" />
            <h2 className="font-bold text-2xl tracking-tight">Cores e Identidade</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="space-y-6">
              <div className="flex items-center justify-between p-5 bg-muted/20 rounded-2xl border border-border shadow-sm">
                <div className="flex items-center gap-3">
                   <Moon className="h-5 w-5 text-skillswap-teal" />
                   <span className="font-bold">Modo Escuro</span>
                </div>
                <Switch checked={darkMode} onCheckedChange={setDarkMode} />
              </div>

              <div className="space-y-3">
                <label className="text-[11px] font-black uppercase tracking-widest text-muted-foreground ml-1">Tema Ativo</label>
                <select 
                  value={customTheme} 
                  onChange={(e) => setCustomTheme(e.target.value)}
                  className="w-full bg-muted/50 border-border rounded-2xl h-14 px-5 font-bold text-base outline-none focus:ring-2 focus:ring-skillswap-teal appearance-none cursor-pointer"
                >
                  <option value="default">Original SkillSwap (Padrão)</option>
                  <option value="cr7">CR7 Premium (Ouro & Marinho)</option>
                  <option value="custom">Painel Customizado</option>
                </select>
              </div>
            </div>

            <div className="space-y-4">
              {customTheme === 'custom' ? (
                <div className="grid grid-cols-2 gap-4 p-6 bg-skillswap-teal/5 rounded-[24px] border-2 border-dashed border-skillswap-teal/20 animate-in zoom-in-95">
                  {[
                    { label: 'Destaque', val: brandColor, set: setBrandColor },
                    { label: 'Fundo', val: bgColor, set: setBgColor },
                    { label: 'Cards', val: navColor, set: setNavColor },
                    { label: 'Texto', val: textColor, set: setTextColor },
                  ].map((color) => (
                    <div key={color.label} className="space-y-2">
                      <label className="text-[10px] font-black uppercase text-muted-foreground">{color.label}</label>
                      <input type="color" value={color.val} onChange={(e) => color.set(e.target.value)} className="w-full h-12 rounded-xl cursor-pointer bg-card border border-border" />
                    </div>
                  ))}
                </div>
              ) : (
                <div className="h-full flex items-center justify-center p-8 bg-muted/10 rounded-[24px] border border-dashed border-border opacity-50">
                  <p className="text-sm font-medium text-center italic">Selecione "Painel Customizado" para liberar os seletores de cores.</p>
                </div>
              )}
            </div>
          </div>
        </Card>

        {/* COLUNA 2: ACESSIBILIDADE E TIPOGRAFIA */}
        <div className="space-y-6">
          <Card className="p-8 border-border bg-card shadow-xl rounded-[32px]">
            <div className="flex items-center gap-3 mb-6">
              <Eye className="text-skillswap-teal h-6 w-6" />
              <h2 className="font-bold text-xl">Visão</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 rounded-2xl border border-border bg-muted/20">
                <div>
                  <p className="font-bold">Alto contraste</p>
                  <p className="text-xs text-muted-foreground">Fundo escuro, letras destacadas e interativos com bordas visíveis.</p>
                </div>
                <Switch checked={highContrast} onCheckedChange={setHighContrast} aria-label="Ativar alto contraste" />
              </div>
              <label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Filtros de Cor</label>
              <select 
                value={colorBlindMode} 
                onChange={(e) => setColorBlindMode(e.target.value)}
                className="w-full bg-muted/50 border-border rounded-xl h-12 px-4 font-bold text-sm outline-none"
              >
                <option value="none">Nenhum (Padrão)</option>
                <option value="protanopia">Protanopia</option>
                <option value="deuteranopia">Deuteranopia</option>
                <option value="tritanopia">Tritanopia</option>
              </select>
            </div>
          </Card>

          <Card className="p-8 border-border bg-card shadow-xl rounded-[32px]">
            <div className="flex items-center gap-3 mb-3">
              <Eye className="text-skillswap-teal h-6 w-6" />
              <h2 className="font-bold text-xl">Marcador visual</h2>
            </div>
            <p className="text-xs text-muted-foreground mb-5">
              Escolha uma combinação de cor e forma para sua apresentação. É uma preferência opcional e não informa deficiência.
            </p>
            <div className="space-y-2">
              {VISUAL_MARKERS.map((marker) => (
                <button
                  key={marker.value}
                  type="button"
                  onClick={() => setVisualMarker(marker.value)}
                  aria-pressed={visualMarker === marker.value}
                  className={`w-full flex items-center justify-between gap-3 rounded-xl border px-4 py-3 text-left transition-colors ${visualMarker === marker.value ? 'border-skillswap-teal bg-skillswap-teal/10' : 'border-border bg-muted/20 hover:bg-muted'}`}
                >
                  <VisualMarker marker={marker.value} showLabel />
                  {visualMarker === marker.value && <Check size={16} aria-label="Selecionado" />}
                </button>
              ))}
            </div>
            <label className="mt-5 flex items-start gap-3 rounded-xl border border-border bg-muted/20 p-3 text-sm">
              <input type="checkbox" checked={visualMarkerPublic} onChange={(event) => setVisualMarkerPublic(event.target.checked)} className="mt-1 h-4 w-4" />
              <span><strong>Exibir no perfil público.</strong><br /><span className="text-xs text-muted-foreground">Desmarcado por padrão. O marcador pode revelar uma preferência de acessibilidade; só o mostre se você quiser.</span></span>
            </label>
          </Card>

          <Card className="p-8 border-border bg-card shadow-xl rounded-[32px]">
            <div className="flex items-center gap-3 mb-6">
              <Type className="text-skillswap-teal h-6 w-6" />
              <h2 className="font-bold text-xl">Tipografia</h2>
            </div>

            <div className="space-y-6">
              <div className="space-y-3">
                <div className="flex gap-2">
                  {['small', 'medium', 'large'].map((size) => (
                    <button 
                      key={size}
                      onClick={() => setFontSize(size)}
                      className={`flex-1 h-10 rounded-lg border text-xs font-bold transition-all ${fontSize === size ? 'bg-skillswap-teal text-white' : 'bg-muted/30 border-border hover:bg-muted'}`}
                    >
                      {size === 'medium' ? 'Médio' : size === 'small' ? 'Pequeno' : 'Grande'}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-3">
                <label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Estilo de Fonte</label>
                <div className="grid grid-cols-1 gap-2 max-h-[160px] overflow-y-auto pr-2 no-scrollbar border-t border-border pt-4">
                  {availableFonts.map((font) => (
                    <button 
                      key={font}
                      onClick={() => setFontFamily(font)}
                      style={{ fontFamily: font }}
                      className={`flex items-center justify-between px-4 py-2.5 rounded-xl border text-sm transition-all ${fontFamily === font ? 'bg-skillswap-teal/10 border-skillswap-teal text-skillswap-teal' : 'bg-muted/20 border-transparent hover:border-border'}`}
                    >
                      <span>{font}</span>
                      {fontFamily === font && <Check size={14} />}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <div className="mt-12 flex flex-col items-center gap-4">
        <Button 
          onClick={saveAccessibilityPreferences}
          className="bg-skillswap-teal hover:bg-teal-700 text-white font-black px-16 py-8 rounded-[24px] text-xl shadow-2xl hover:scale-105 transition-all"
        >
          APLICAR PREFERÊNCIAS
        </Button>
        <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest flex items-center gap-2">
          <Monitor size={12} /> Sincronizado com todos os dispositivos
        </p>
      </div>
    </div>
  );
};

export default PreferencesPage;
