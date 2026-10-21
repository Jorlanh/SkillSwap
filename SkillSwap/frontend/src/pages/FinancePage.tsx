import { useEffect, useState } from 'react';
import { FinanceService, PremiumService } from '@/services/api';
import { ChevronLeft, History, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Skeleton } from '@/components/ui/skeleton';

const financeTabs = ['Recursos gratuitos', 'Histórico'];

type LedgerEntry = {
  id: number;
  type: string;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
};

const FinancePage = () => {
  const [activeTab, setActiveTab] = useState('Recursos gratuitos');
  const [history, setHistory] = useState<LedgerEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [aiUsage, setAiUsage] = useState<{ remaining: number; monthlyQuota: number; paidCostAfterQuota: number } | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      try {
        const [historyRes] = await Promise.all([
          FinanceService.getHistory(),
        ]);
        setHistory(historyRes.data.content ?? []);
      } catch (error) {
        console.error('Falha na sincronização financeira:', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, []);

  useEffect(() => {
    PremiumService.getCatalog().catch(() => undefined);
    PremiumService.getAiUsage?.().then((response) => setAiUsage(response.data)).catch(() => undefined);
  }, []);

  return (
    <div className="max-w-5xl mx-auto p-4">
      <div className="bg-card rounded-3xl p-8 border border-border shadow-sm">
        <div className="flex items-center justify-between mb-8">
          <Link to="/" className="flex items-center gap-2 text-muted-foreground hover:text-skillswap-teal font-bold transition-colors">
            <ChevronLeft size={20} /> Voltar
          </Link>
          <div className="bg-skillswap-teal/10 text-skillswap-teal px-4 py-1 rounded-full text-xs font-black uppercase tracking-widest">Acesso gratuito</div>
        </div>

        <h1 className="font-display font-black text-4xl text-foreground text-center mb-3">SkillSwap gratuito</h1>
        <p className="mb-10 text-center text-sm text-muted-foreground">A plataforma não cobra usuários. Troca de habilidades, aulas, chat, certificados e acessibilidade ficam disponíveis sem checkout.</p>

        <div className="mb-8 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="rounded-2xl border border-border bg-background p-5">
            <p className="text-xs font-black uppercase tracking-widest text-muted-foreground">Assistente ELOS</p>
            <p className="mt-2 text-sm font-bold">{aiUsage ? `${aiUsage.remaining}/${aiUsage.monthlyQuota} consultas disponíveis` : 'Consultas disponíveis'}</p>
            <p className="text-xs text-muted-foreground">Nesta versão, não há cobrança após a cota.</p>
          </div>
          <div className="rounded-2xl border border-border bg-background p-5"><p className="text-xs font-black uppercase tracking-widest text-muted-foreground">Recursos incluídos</p><p className="mt-2 text-sm font-bold">Aulas, chat, certificados e recursos de acessibilidade sem pagamento.</p></div>
        </div>

        <div className="flex flex-col md:flex-row gap-8">
          <div className="w-full md:w-56 shrink-0 space-y-2">
            {financeTabs.map((tab) => (
              <button
                key={tab}
                className={`w-full text-left px-6 py-3 rounded-2xl text-sm font-bold transition-all ${
                  activeTab === tab
                    ? 'bg-skillswap-teal text-white shadow-lg shadow-skillswap-teal/20'
                    : 'bg-muted/50 text-muted-foreground hover:bg-muted'
                }`}
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="flex-1">
            {activeTab === 'Recursos gratuitos' && (
              <div className="rounded-3xl border-2 border-dashed border-border bg-background p-10 text-center">
                {isLoading ? <Skeleton className="mx-auto h-20 w-full max-w-md rounded-2xl" /> : <><h2 className="text-2xl font-black">Sem planos pagos</h2><p className="mx-auto mt-3 max-w-xl text-sm text-muted-foreground">SwapCoins e checkout estão desativados. O saldo exibido é apenas um registro interno legado e não é necessário para usar a plataforma.</p></>}
              </div>
            )}

            {activeTab === 'Histórico' && (
              <div className="space-y-3">
                {history.length > 0 ? history.map((item) => (
                  <div key={item.id} className="flex items-center justify-between p-4 rounded-2xl bg-muted/30 border border-border/50">
                    <div className="flex items-center gap-4">
                      <div className={`p-2 rounded-full ${item.amount > 0 ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'}`}>
                        <CheckCircle2 size={18} />
                      </div>
                      <div>
                        <p className="font-bold text-sm">{item.description}</p>
                        <p className="text-[10px] uppercase font-bold text-muted-foreground">
                          {new Date(item.createdAt).toLocaleDateString('pt-BR')} · saldo: {item.balanceAfter} SC
                        </p>
                      </div>
                    </div>
                    <p className={`font-black ${item.amount > 0 ? 'text-green-500' : 'text-red-500'}`}>
                      {item.amount > 0 ? '+' : ''}{item.amount} SC
                    </p>
                  </div>
                )) : (
                  <div className="text-center py-20 bg-muted/20 rounded-3xl border-2 border-dashed border-border">
                    <History size={40} className="mx-auto mb-4 opacity-20" />
                    <p className="text-muted-foreground font-bold">Sem registros de transação.</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FinancePage;
