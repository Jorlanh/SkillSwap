import { useState } from 'react';
import { mockAccessLogs } from '@/data/mockData';
import { Switch } from '@/components/ui/switch';
import { ChevronRight, Clock, Camera, Mic, MapPin, Users, Shield, Key, Lock } from 'lucide-react';
import { Link } from 'react-router-dom';

type SettingsTab = 'historico' | 'permissoes' | 'seguranca';

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('historico');

  const [permissions, setPermissions] = useState({
    camera: true,
    microfone: true,
    localizacao: false,
    contatos: false,
  });

  return (
    <div className="max-w-3xl mx-auto">
      {/* Tab navigation */}
      <div className="flex gap-3 mb-6">
        {[
          { id: 'historico' as const, label: 'Histórico de Acesso' },
          { id: 'permissoes' as const, label: 'Permissões' },
          { id: 'seguranca' as const, label: 'Segurança' },
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
      </div>
    </div>
  );
};

export default SettingsPage;
