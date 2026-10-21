import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { AdminService } from '@/services/api';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ShieldCheck, UserX, UserCheck, Activity, Users, Zap, ArrowLeft, KeyRound, Coins, BarChart3, ReceiptText } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

type Stats = { totalUsers: number; totalSkills: number; totalProposals: number; pendingProposals: number; completedProposals: number };
type OperationsReport = {
  totalWallets: number; totalBalance: number; paidPayments: number; pendingPayments: number; failedPayments: number;
  totalRevenueCents: number; totalPurchasedCoins: number; totalLedgerEntries: number; totalCredits: number; totalDebits: number;
  featureSpendCoins: number; tipVolumeCoins: number; lessonPaymentCoins: number; achievementRewardCoins: number;
  premiumAiPrompts: number; extraLessonPurchases: number; paidLessonCoins: number; totalKarmaPoints: number;
  activeBoostedProposals: number; activeHighlightedPosts: number; portfolioSlots: number; rabbitMqEnabled: boolean;
};
type LedgerEntry = { id: number; type: string; amount: number; balanceAfter: number; description: string; createdAt: string };
type ManagedUser = {
  userId: string;
  username: string;
  name: string;
  email: string;
  verifiedBadge: boolean;
  roles: string[];
  banned: boolean;
  createdAt: string;
  walletBalance: number;
  karmaPoints: number;
};
type ModerationCase = {
  reportId: number;
  reporterUsername: string;
  reportedUserId: string;
  reportedUsername: string;
  reportedUserIsAdmin: boolean;
  reason: string;
  reportedContent?: string;
  source?: string;
  status: string;
  aiCategory?: string;
  aiConfidence?: number;
  aiRecommendation?: string;
  contentContained: boolean;
  action?: string;
  decisionReason?: string;
  appealStatus?: string;
  appealReason?: string;
  automaticTemporaryBanApplied: boolean;
  automaticActionAt?: string;
};
type ModerationPolicy = { automaticTemporaryBanEnabled: boolean; automaticTemporaryBanHours: number };

const AdminConsole = () => {
  const [stats, setStats] = useState<Stats | null>(null);
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [report, setReport] = useState<OperationsReport | null>(null);
  const [ledger, setLedger] = useState<LedgerEntry[]>([]);
  const [moderationCases, setModerationCases] = useState<ModerationCase[]>([]);
  const [moderationPolicy, setModerationPolicy] = useState<ModerationPolicy>({ automaticTemporaryBanEnabled: false, automaticTemporaryBanHours: 24 });
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes, reportRes, ledgerRes, casesRes, policyRes] = await Promise.all([
        AdminService.getStats(), AdminService.getUsers(), AdminService.getOperationsReport(), AdminService.getLedgerAudit(),
        AdminService.getModerationCases(), AdminService.getModerationPolicy(),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data);
      setReport(reportRes.data);
      setLedger(ledgerRes.data.content || []);
      setModerationCases(casesRes.data || []);
      setModerationPolicy(policyRes.data);
    } catch {
      toast({ title: 'Acesso negado', description: 'Sua conta não possui a função ADMIN.', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { loadAdminData(); }, [loadAdminData]);

  const handleToggleVerify = async (userId: string) => {
    try {
      await AdminService.toggleVerification(userId);
      toast({ title: 'Selo atualizado', description: 'O status de verificação foi alterado.' });
      loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível alterar o selo.', variant: 'destructive' }); }
  };

  const handleBan = async (userId: string, banned: boolean) => {
    try {
      if (banned) await AdminService.unbanUser(userId);
      else await AdminService.banUser(userId, { reason: 'Ação administrativa pela plataforma', expiresAt: null, ipAddress: null });
      toast({ title: banned ? 'Usuário reativado' : 'Usuário banido' });
      loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível atualizar o banimento.', variant: 'destructive' }); }
  };

  const handleRole = async (user: ManagedUser) => {
    const enabled = !user.roles.some((role) => role.toUpperCase() === 'ADMIN');
    try {
      await AdminService.setRole(user.userId, 'ADMIN', enabled);
      toast({ title: enabled ? 'Administrador adicionado' : 'Administrador removido' });
      loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível atualizar a função.', variant: 'destructive' }); }
  };

  const handleWalletAdjustment = async (user: ManagedUser) => {
    const rawAmount = window.prompt('Ajuste de SwapCoins (positivo credita, negativo debita):', '0');
    if (rawAmount === null) return;
    const amount = Number(rawAmount);
    if (!Number.isInteger(amount) || amount === 0) {
      toast({ title: 'Valor inválido', description: 'Informe um número inteiro diferente de zero.', variant: 'destructive' });
      return;
    }
    const reason = window.prompt('Motivo do ajuste auditável:', 'Ajuste administrativo');
    if (!reason?.trim()) return;
    try {
      await AdminService.adjustWallet(user.userId, amount, reason.trim());
      toast({ title: 'Carteira ajustada', description: `${amount > 0 ? '+' : ''}${amount} SwapCoins lançados no ledger.` });
      loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível ajustar a carteira.', variant: 'destructive' }); }
  };

  const handlePolicyChange = async (enabled: boolean) => {
    try {
      const response = await AdminService.updateModerationPolicy({ ...moderationPolicy, automaticTemporaryBanEnabled: enabled });
      setModerationPolicy(response.data);
      toast({ title: enabled ? 'Contenção temporária automática ativada' : 'Modo somente revisão ativado', description: enabled ? 'A IA poderá conter contas não administrativas por até 24 horas, sempre com revisão humana posterior.' : 'A IA apenas classifica e contém conteúdo; nenhuma conta será suspensa automaticamente.' });
    } catch { toast({ title: 'Erro', description: 'Não foi possível atualizar a política de moderação.', variant: 'destructive' }); }
  };

  const handleModerationDecision = async (item: ModerationCase) => {
    const action = window.prompt('Ação: RELEASE, WARNING, SUSPEND_DAYS, SUSPEND_MONTHS ou PERMANENT_BAN', 'RELEASE')?.trim().toUpperCase();
    if (!action || !['RELEASE', 'WARNING', 'SUSPEND_DAYS', 'SUSPEND_MONTHS', 'PERMANENT_BAN'].includes(action)) return;
    const reason = window.prompt('Justificativa obrigatória da decisão humana:');
    if (!reason?.trim()) return;
    let durationDays: number | undefined;
    if (action === 'SUSPEND_DAYS' || action === 'SUSPEND_MONTHS') {
      const raw = window.prompt(action === 'SUSPEND_DAYS' ? 'Quantidade de dias (1 a 31):' : 'Quantidade de dias equivalentes aos meses (30 a 3650):', action === 'SUSPEND_DAYS' ? '1' : '30');
      durationDays = Number(raw);
      if (!Number.isInteger(durationDays) || durationDays < 1) return;
    }
    const evidence = window.prompt('Evidência ou referência para auditoria (opcional):', item.reportedContent || item.reason) || '';
    try {
      await AdminService.decideModeration(item.reportId, { action: action as any, reason: reason.trim(), evidence, durationDays, humanConfirmation: true });
      toast({ title: 'Decisão registrada', description: 'A decisão humana foi salva com justificativa e evidência.' });
      loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível registrar a decisão.', variant: 'destructive' }); }
  };

  return (
    <div className="p-8 space-y-8 bg-zinc-950 min-h-screen text-white">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-red-400 font-black">Área protegida</p>
          <h1 className="text-3xl font-bold flex items-center gap-3"><ShieldCheck className="text-red-500" size={32} /> Painel do administrador</h1>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant="outline" className="text-red-500 border-red-500">ADMIN</Badge>
          <Link to="/" className="inline-flex items-center gap-2 text-sm text-zinc-300 hover:text-white"><ArrowLeft size={16} /> Voltar ao usuário</Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <StatCard title="Usuários" value={stats?.totalUsers ?? 0} icon={<Users />} />
        <StatCard title="Propostas" value={stats?.totalProposals ?? 0} icon={<Activity />} />
        <StatCard title="Pendentes" value={stats?.pendingProposals ?? 0} icon={<Zap />} />
        <StatCard title="Concluídas" value={stats?.completedProposals ?? 0} icon={<KeyRound />} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <StatCard title="Saldo em circulação" value={`${report?.totalBalance ?? 0} SC`} icon={<Coins />} />
        <StatCard title="Receita Stripe" value={`R$ ${((report?.totalRevenueCents ?? 0) / 100).toFixed(2)}`} icon={<ReceiptText />} />
        <StatCard title="Karma distribuído" value={report?.totalKarmaPoints ?? 0} icon={<BarChart3 />} />
        <StatCard title="Consultas IA" value={report?.premiumAiPrompts ?? 0} icon={<Activity />} />
      </div>

      <Card className="bg-zinc-900 border-zinc-800 text-white">
        <CardHeader><CardTitle>Relatório financeiro e métricas premium</CardTitle></CardHeader>
        <CardContent className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <Metric label="Carteiras" value={report?.totalWallets} />
          <Metric label="Compras pagas" value={report?.paidPayments} />
          <Metric label="Pagamentos pendentes" value={report?.pendingPayments} />
          <Metric label="Falhas de pagamento" value={report?.failedPayments} />
          <Metric label="SwapCoins comprados" value={report?.totalPurchasedCoins} />
          <Metric label="Ledger: créditos" value={report?.totalCredits} />
          <Metric label="Ledger: débitos" value={report?.totalDebits} />
          <Metric label="Gastos premium" value={report?.featureSpendCoins} />
          <Metric label="Volume de gorjetas" value={report?.tipVolumeCoins} />
          <Metric label="Aulas extras" value={report?.extraLessonPurchases} />
          <Metric label="Boosts ativos" value={report?.activeBoostedProposals} />
          <Metric label="Posts destacados" value={report?.activeHighlightedPosts} />
          <Metric label="Slots de portfólio" value={report?.portfolioSlots} />
          <Metric label="RabbitMQ" value={report?.rabbitMqEnabled ? 'Ativo' : 'Fallback síncrono'} />
        </CardContent>
      </Card>

      <Card className="bg-zinc-900 border-zinc-800 text-white">
        <CardHeader><CardTitle>Auditoria do ledger (últimos lançamentos)</CardTitle></CardHeader>
        <CardContent>
          <Table><TableHeader><TableRow className="border-zinc-800"><TableHead className="text-zinc-400">Data</TableHead><TableHead className="text-zinc-400">Tipo</TableHead><TableHead className="text-zinc-400">Descrição</TableHead><TableHead className="text-zinc-400 text-right">Valor</TableHead></TableRow></TableHeader>
            <TableBody>{ledger.map((entry) => <TableRow key={entry.id} className="border-zinc-800"><TableCell className="text-xs text-zinc-500">{new Date(entry.createdAt).toLocaleString('pt-BR')}</TableCell><TableCell><Badge variant="outline">{entry.type}</Badge></TableCell><TableCell className="max-w-md truncate">{entry.description}</TableCell><TableCell className={entry.amount >= 0 ? 'text-green-400 text-right' : 'text-red-400 text-right'}>{entry.amount > 0 ? '+' : ''}{entry.amount} SC</TableCell></TableRow>)}</TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card className="bg-zinc-900 border-zinc-800 text-white">
        <CardHeader>
          <CardTitle>Moderação assistida e fila de revisão</CardTitle>
          <p className="text-sm text-zinc-400">A IA classifica e contém conteúdo. Suspensões e banimentos dependem de decisão humana registrada.</p>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-zinc-800 bg-zinc-950 p-4">
            <div>
              <p className="font-semibold">Contenção temporária automática de conta</p>
              <p className="text-xs text-zinc-500">Quando ativa, somente casos classificados como risco podem gerar suspensão temporária de até 24h. Administradores nunca entram nessa regra e o caso permanece na fila.</p>
            </div>
            <Button variant={moderationPolicy.automaticTemporaryBanEnabled ? 'destructive' : 'outline'} onClick={() => handlePolicyChange(!moderationPolicy.automaticTemporaryBanEnabled)}>
              {moderationPolicy.automaticTemporaryBanEnabled ? 'Desativar automático' : 'Ativar temporário'}
            </Button>
          </div>
          {moderationCases.length === 0 ? <p className="text-sm text-zinc-500">Nenhum caso aguardando revisão.</p> : (
            <div className="space-y-3">
              {moderationCases.map((item) => <div key={item.reportId} className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div><p className="font-semibold">Caso #{item.reportId}: @{item.reportedUsername}</p><p className="text-xs text-zinc-500">{item.source === 'AI_CONTENT_BLOCK' ? 'Conteúdo contido pela IA' : `Denunciado por @${item.reporterUsername}`} · status {item.status}</p></div>
                  <div className="flex gap-2"><Badge variant="outline">IA: {item.aiCategory || 'não classificado'}</Badge>{item.reportedUserIsAdmin && <Badge className="bg-red-700">Conta ADMIN protegida</Badge>}{item.automaticTemporaryBanApplied && <Badge className="bg-amber-600">Contenção temporária aplicada</Badge>}</div>
                </div>
                <p className="text-sm text-zinc-300">{item.reportedContent || item.reason}</p>
                <div className="flex flex-wrap gap-2"><Button size="sm" onClick={() => handleModerationDecision(item)}>Registrar decisão humana</Button>{item.appealStatus === 'PENDING' && <Badge variant="outline">Recurso pendente</Badge>}</div>
              </div>)}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="bg-zinc-900 border-zinc-800 text-white">
        <CardHeader><CardTitle>Gestão de usuários, papéis e segurança</CardTitle></CardHeader>
        <CardContent>
          {loading ? <p className="text-zinc-400 py-8">Carregando dados protegidos...</p> : (
            <Table>
              <TableHeader><TableRow className="border-zinc-800"><TableHead className="text-zinc-400">Usuário</TableHead><TableHead className="text-zinc-400">Papéis</TableHead><TableHead className="text-zinc-400">Saldo/Karma</TableHead><TableHead className="text-zinc-400">Verificação</TableHead><TableHead className="text-zinc-400 text-right">Ações</TableHead></TableRow></TableHeader>
              <TableBody>{users.map((user) => {
                const isAdmin = user.roles.some((role) => role.toUpperCase() === 'ADMIN');
                return <TableRow key={user.userId} className="border-zinc-800">
                  <TableCell><p className="font-medium">{user.name || user.username}</p><p className="text-xs text-zinc-500">{user.email}</p></TableCell>
                  <TableCell><div className="flex gap-1 flex-wrap">{user.roles.map((role) => <Badge key={role} variant="outline">{role}</Badge>)}</div></TableCell>
                  <TableCell><p className="text-sm text-yellow-300">{user.walletBalance} SC</p><p className="text-xs text-zinc-500">{user.karmaPoints} Karma</p></TableCell>
                  <TableCell>{user.verifiedBadge ? <Badge className="bg-green-600">Verificado</Badge> : <Badge variant="outline">Pendente</Badge>}</TableCell>
                  <TableCell className="text-right space-x-2 space-y-2">
                    <Button size="sm" variant="outline" onClick={() => handleToggleVerify(user.userId)}>{user.verifiedBadge ? 'Remover selo' : 'Dar selo'}</Button>
                    <Button size="sm" variant={isAdmin ? 'destructive' : 'outline'} onClick={() => handleRole(user)}><KeyRound size={14} className="mr-1" />{isAdmin ? 'Remover admin' : 'Tornar admin'}</Button>
                    <Button size="sm" variant="outline" onClick={() => handleWalletAdjustment(user)}><Coins size={14} className="mr-1" />Ajustar SC</Button>
                    {user.banned ? <Button size="sm" variant="outline" onClick={() => handleBan(user.userId, true)}><UserCheck size={14} className="mr-1" />Reativar</Button> : <Button size="sm" variant="destructive" onClick={() => handleBan(user.userId, false)}><UserX size={14} className="mr-1" />Banir</Button>}
                  </TableCell>
                </TableRow>;
              })}</TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

const StatCard = ({ title, value, icon }: { title: string; value: number | string; icon: ReactNode }) => (
  <Card className="bg-zinc-900 border-zinc-800 text-white"><CardHeader className="flex flex-row items-center justify-between pb-2"><CardTitle className="text-sm font-medium text-zinc-400">{title}</CardTitle><div className="text-zinc-500">{icon}</div></CardHeader><CardContent><div className="text-2xl font-bold">{value}</div></CardContent></Card>
);

const Metric = ({ label, value }: { label: string; value?: number | string }) => (
  <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"><p className="text-xs text-zinc-500">{label}</p><p className="mt-1 font-bold">{value ?? 0}</p></div>
);

export default AdminConsole;
