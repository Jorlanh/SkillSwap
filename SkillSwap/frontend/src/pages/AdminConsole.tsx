import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AccessService, AdminReportService, AdminService } from '@/services/api';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ShieldCheck, UserX, UserCheck, Activity, Users, Zap, ArrowLeft, KeyRound, BriefcaseBusiness, Trash2, CalendarDays } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

type Stats = { totalUsers: number; totalSkills: number; totalProposals: number; pendingProposals: number; completedProposals: number };
type ManagedUser = {
  userId: string;
  username: string;
  name: string;
  email: string;
  verifiedBadge: boolean;
  roles: string[];
  banned: boolean;
  createdAt: string;
};
type ActiveBan = { id: number; userId: string; username: string; reason: string; scope: string; createdAt?: string; expiresAt?: string };
type AdminReport = { id: number; reporterUsername: string; reportedUserId: string; reportedUsername: string; reason: string; status: string; createdAt?: string; targetType?: string; targetId?: string; category?: string };
type ManagedCommunity = { communityId: string; name: string; description?: string; memberCount?: number; ownerId?: string; privateCommunity?: boolean };
type CommunityMember = { userId: string; username: string; name?: string; role?: string; status?: string };
type Opportunity = { opportunityId: string; organizationName: string; title: string; description: string; requiredSkills: string; participationMode?: string; location?: string; contactUrl?: string; };

const AdminConsole = () => {
  const [stats, setStats] = useState<Stats | null>(null);
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [activeBans, setActiveBans] = useState<ActiveBan[]>([]);
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [communities, setCommunities] = useState<ManagedCommunity[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [opportunityForm, setOpportunityForm] = useState({ organizationName: '', title: '', description: '', requiredSkills: '', participationMode: 'REMOTE', location: '', contactUrl: '', accessibleSupport: true });
  const [suspensionTarget, setSuspensionTarget] = useState<ManagedUser | null>(null);
  const [suspensionUntil, setSuspensionUntil] = useState('');
  const [suspensionScope, setSuspensionScope] = useState('GENERAL');
  const [suspensionIp, setSuspensionIp] = useState('');
  const [suspensionReason, setSuspensionReason] = useState('');
  const [reportResponses, setReportResponses] = useState<Record<number, string>>({});
  const [transferTarget, setTransferTarget] = useState<ManagedCommunity | null>(null);
  const [transferMembers, setTransferMembers] = useState<CommunityMember[]>([]);
  const [selectedOwnerId, setSelectedOwnerId] = useState('');
  const [loadingTransferMembers, setLoadingTransferMembers] = useState(false);
  const [userSearch, setUserSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    const results = await Promise.allSettled([
      AdminService.getStats(),
      AdminService.getUsers(),
      AdminService.getActiveBans(),
      AccessService.getOpportunities(),
      AdminReportService.getPending(),
      AdminService.getCommunities(),
    ]);

    const [statsRes, usersRes, bansRes, opportunitiesRes, reportsRes, communitiesRes] = results;
    if (statsRes.status === 'fulfilled') setStats(statsRes.value.data);
    if (usersRes.status === 'fulfilled') setUsers(usersRes.value.data);
    if (bansRes.status === 'fulfilled') setActiveBans(Array.isArray(bansRes.value.data) ? bansRes.value.data : []);
    if (opportunitiesRes.status === 'fulfilled') setOpportunities(Array.isArray(opportunitiesRes.value.data) ? opportunitiesRes.value.data : []);
    if (reportsRes.status === 'fulfilled') setReports(Array.isArray(reportsRes.value.data) ? reportsRes.value.data : []);
    if (communitiesRes.status === 'fulfilled') setCommunities(Array.isArray(communitiesRes.value.data) ? communitiesRes.value.data : []);

    const failures = results.filter((result) => result.status === 'rejected');
    if (failures.length > 0) {
      const authenticationFailure = failures.some((result) => {
        if (result.status !== 'rejected') return false;
        const status = (result.reason as { response?: { status?: number } })?.response?.status;
        return status === 401 || status === 403;
      });
      toast({
        title: authenticationFailure ? 'Acesso negado' : 'Dados parcialmente indisponíveis',
        description: authenticationFailure
          ? 'Sua conta precisa ter a função ADMIN para acessar o painel.'
          : 'Algumas informações não puderam ser carregadas. As demais continuam disponíveis.',
        variant: 'destructive',
      });
    }
    setLoading(false);
  }, [toast]);

  useEffect(() => { loadAdminData(); }, [loadAdminData]);

  const handleToggleVerify = async (userId: string) => {
    try {
      await AdminService.toggleVerification(userId);
      toast({ title: 'Selo atualizado', description: 'O status de verificação foi alterado.' });
      loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível alterar o selo.', variant: 'destructive' });
    }
  };

  const createOpportunity = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      await AccessService.createOpportunity({
        ...opportunityForm,
        remote: opportunityForm.participationMode === 'REMOTE',
        contactUrl: opportunityForm.contactUrl || null,
        location: opportunityForm.location || null,
      });
      setOpportunityForm({ organizationName: '', title: '', description: '', requiredSkills: '', participationMode: 'REMOTE', location: '', contactUrl: '', accessibleSupport: true });
      toast({ title: 'Oportunidade publicada', description: 'Ela já aparece na Minha Jornada.' });
      await loadAdminData();
    } catch { toast({ title: 'Erro', description: 'Não foi possível publicar a oportunidade.', variant: 'destructive' }); }
  };

  const deleteOpportunity = async (opportunityId: string) => {
    if (!window.confirm('Remover esta oportunidade?')) return;
    try { await AccessService.deleteOpportunity(opportunityId); toast({ title: 'Oportunidade removida' }); await loadAdminData(); }
    catch { toast({ title: 'Erro', description: 'Não foi possível remover a oportunidade.', variant: 'destructive' }); }
  };

  const defaultSuspensionDate = () => {
    const date = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return localDate.toISOString().slice(0, 16);
  };

  const openSuspension = (user: ManagedUser) => {
    setSuspensionTarget(user);
    setSuspensionUntil(defaultSuspensionDate());
    setSuspensionScope('GENERAL');
    setSuspensionIp('');
    setSuspensionReason('');
  };

  const submitSuspension = async () => {
    if (!suspensionTarget || !suspensionUntil) {
      toast({ title: 'Informe o prazo', description: 'Escolha a data e a hora de encerramento da suspensão.', variant: 'destructive' });
      return;
    }
    try {
      await AdminService.banUser(suspensionTarget.userId, {
        reason: suspensionReason.trim() || 'Suspensão aplicada pela administração da plataforma.',
        expiresAt: new Date(suspensionUntil).toISOString(),
        scope: suspensionScope,
        ipAddress: suspensionIp.trim() || null,
      });
      toast({ title: 'Suspensão aplicada', description: `A suspensão de @${suspensionTarget.username} foi registrada.` });
      setSuspensionTarget(null);
      await loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível aplicar a suspensão.', variant: 'destructive' });
    }
  };

  const handlePermanentBan = async (user: ManagedUser) => {
    if (!window.confirm(`Aplicar banimento permanente em @${user.username}?`)) return;
    try {
      await AdminService.banUser(user.userId, { reason: 'Banimento permanente aplicado pela administração da plataforma.', expiresAt: null, scope: 'GENERAL', ipAddress: null });
      toast({ title: 'Banimento permanente aplicado' });
      await loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível aplicar o banimento permanente.', variant: 'destructive' });
    }
  };

  const handleUnban = async (userId: string) => {
    try { await AdminService.unbanUser(userId); toast({ title: 'Suspensão removida' }); await loadAdminData(); }
    catch { toast({ title: 'Erro', description: 'Não foi possível remover a suspensão.', variant: 'destructive' }); }
  };

  const reviewReport = async (reportId: number, status: 'REVIEWED' | 'DISMISSED') => {
    try {
      await AdminReportService.review(reportId, status, reportResponses[reportId] || '');
      setReports((current) => current.filter((report) => report.id !== reportId));
      toast({ title: status === 'DISMISSED' ? 'Denúncia arquivada' : 'Denúncia revisada' });
    }
    catch { toast({ title: 'Erro', description: 'Não foi possível atualizar a denúncia.', variant: 'destructive' }); }
  };

  const filteredUsers = users.filter((user) => `${user.name} ${user.username} ${user.email}`.toLowerCase().includes(userSearch.toLowerCase().trim()));

  const handleRole = async (user: ManagedUser) => {
    const enabled = !user.roles.some((role) => role.toUpperCase() === 'ADMIN');
    try {
      await AdminService.setRole(user.userId, 'ADMIN', enabled);
      toast({ title: enabled ? 'Administrador adicionado' : 'Administrador removido' });
      loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível atualizar a função.', variant: 'destructive' });
    }
  };

  const openOwnershipTransfer = async (community: ManagedCommunity) => {
    setTransferTarget(community);
    setSelectedOwnerId('');
    setTransferMembers([]);
    setLoadingTransferMembers(true);
    try {
      const { data } = await AdminService.getCommunityMembers(community.communityId);
      const members = Array.isArray(data) ? data.filter((member: CommunityMember) => {
        const approved = String(member.status || 'APPROVED').toUpperCase() === 'APPROVED';
        return approved && String(member.userId) !== String(community.ownerId || '');
      }) : [];
      setTransferMembers(members);
    } catch {
      setTransferTarget(null);
      toast({ title: 'Erro', description: 'Não foi possível carregar os membros aprovados desta comunidade.', variant: 'destructive' });
    } finally {
      setLoadingTransferMembers(false);
    }
  };

  const transferCommunityOwner = async () => {
    if (!transferTarget || !selectedOwnerId) {
      toast({ title: 'Selecione um membro', description: 'Escolha um membro aprovado para receber a propriedade.', variant: 'destructive' });
      return;
    }
    try {
      await AdminService.transferCommunityOwnership(transferTarget.communityId, selectedOwnerId);
      toast({ title: 'Propriedade transferida', description: 'O novo dono foi salvo no banco de dados.' });
      setTransferTarget(null);
      setSelectedOwnerId('');
      await loadAdminData();
    } catch {
      toast({ title: 'Erro', description: 'Não foi possível transferir a propriedade. O usuário precisa estar aprovado na comunidade.', variant: 'destructive' });
    }
  };

  return (
    <div className="p-4 sm:p-8 space-y-8 bg-white min-h-screen text-slate-900">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-skillswap-teal font-black">Área protegida</p>
          <h1 className="text-3xl font-bold flex items-center gap-3"><ShieldCheck className="text-skillswap-teal" size={32} /> Painel do administrador</h1>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant="outline" className="text-skillswap-teal border-skillswap-teal">ADMIN</Badge>
          <Link to="/" className="inline-flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900"><ArrowLeft size={16} /> Voltar ao usuário</Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <StatCard title="Usuários" value={stats?.totalUsers ?? 0} icon={<Users />} />
        <StatCard title="Propostas" value={stats?.totalProposals ?? 0} icon={<Activity />} />
        <StatCard title="Pendentes" value={stats?.pendingProposals ?? 0} icon={<Zap />} />
        <StatCard title="Concluídas" value={stats?.completedProposals ?? 0} icon={<KeyRound />} />
      </div>

      {suspensionTarget && <Card className="border-amber-300 bg-amber-50 text-slate-900 shadow-sm">
        <CardHeader><CardTitle className="flex items-center gap-2"><CalendarDays className="h-5 w-5" /> Aplicar suspensão em @{suspensionTarget.username}</CardTitle><p className="text-sm text-slate-600">Escolha quando a suspensão termina. Para bloquear sem prazo, use o botão de banimento permanente.</p></CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2">
          <label className="text-sm font-medium">Suspender até<input type="datetime-local" value={suspensionUntil} onChange={(event) => setSuspensionUntil(event.target.value)} className="mt-1 block w-full rounded border border-slate-300 bg-white px-3 py-2" /></label>
          <label className="text-sm font-medium">Alcance<select value={suspensionScope} onChange={(event) => setSuspensionScope(event.target.value)} className="mt-1 block w-full rounded border border-slate-300 bg-white px-3 py-2"><option value="GENERAL">Conta inteira</option><option value="POSTS">Publicações</option><option value="COMMENTS">Comentários</option></select></label>
          <label className="text-sm font-medium">IP do dispositivo (opcional)<input value={suspensionIp} onChange={(event) => setSuspensionIp(event.target.value)} placeholder="Ex.: 203.0.113.10" className="mt-1 block w-full rounded border border-slate-300 bg-white px-3 py-2" /></label>
          <label className="text-sm font-medium md:col-span-2">Motivo<textarea value={suspensionReason} onChange={(event) => setSuspensionReason(event.target.value)} rows={2} placeholder="Explique o motivo da suspensão" className="mt-1 block w-full rounded border border-slate-300 bg-white px-3 py-2" /></label>
          <div className="flex flex-wrap gap-2 md:col-span-2"><Button type="button" onClick={() => void submitSuspension()} className="bg-amber-600 text-white hover:bg-amber-700">Confirmar suspensão</Button><Button type="button" variant="outline" onClick={() => setSuspensionTarget(null)}>Cancelar</Button></div>
        </CardContent>
      </Card>}

      <Card className="bg-white border-slate-200 text-slate-900 shadow-sm">
        <CardHeader><CardTitle>Gestão de usuários, selos e segurança</CardTitle><p className="text-sm text-slate-500">A suspensão abre um calendário para escolher o prazo. O banimento permanente fica separado.</p><input value={userSearch} onChange={(event) => setUserSearch(event.target.value)} placeholder="Pesquisar usuário por nome, @usuário ou e-mail" className="mt-3 w-full max-w-xl rounded border px-3 py-2" /></CardHeader>
        <CardContent>
          {loading ? <p className="text-slate-500 py-8">Carregando dados protegidos...</p> : (
            <Table>
              <TableHeader><TableRow className="border-zinc-800"><TableHead className="text-zinc-400">Usuário</TableHead><TableHead className="text-zinc-400">Papéis</TableHead><TableHead className="text-zinc-400">Verificação</TableHead><TableHead className="text-zinc-400 text-right">Ações</TableHead></TableRow></TableHeader>
              <TableBody>{filteredUsers.map((user) => {
                const isAdmin = user.roles.some((role) => role.toUpperCase() === 'ADMIN');
                return <TableRow key={user.userId} className="border-zinc-800">
                  <TableCell><p className="font-medium">{user.name || user.username}</p><p className="text-xs text-zinc-500">{user.email}</p></TableCell>
                  <TableCell><div className="flex gap-1 flex-wrap">{user.roles.map((role) => <Badge key={role} variant="outline">{role}</Badge>)}</div></TableCell>
                  <TableCell>{user.verifiedBadge ? <Badge className="bg-green-600">Verificado</Badge> : <Badge variant="outline">Pendente</Badge>}</TableCell>
                  <TableCell className="text-right space-x-2 space-y-2">
                    <Button size="sm" variant="outline" onClick={() => handleToggleVerify(user.userId)}>{user.verifiedBadge ? 'Remover selo' : 'Dar selo'}</Button>
                    <Button size="sm" variant={isAdmin ? 'destructive' : 'outline'} onClick={() => handleRole(user)}><KeyRound size={14} className="mr-1" />{isAdmin ? 'Remover admin' : 'Tornar admin'}</Button>
                    {user.banned ? <Button size="sm" variant="outline" onClick={() => void handleUnban(user.userId)}><UserCheck size={14} className="mr-1" />Reativar</Button> : <><Button size="sm" variant="outline" title="Aplicar suspensão personalizada" onClick={() => openSuspension(user)}><CalendarDays size={14} className="mr-1" />Suspensão</Button><Button size="sm" variant="destructive" title="Aplicar banimento permanente" onClick={() => void handlePermanentBan(user)}><UserX size={14} className="mr-1" />Banimento permanente</Button></>}
                  </TableCell>
                </TableRow>;
              })}</TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      <Card className="bg-white border-slate-200 text-slate-900 shadow-sm">
        <CardHeader><CardTitle>Suspensões ativas e ações automáticas</CardTitle><p className="text-sm text-slate-500">Aqui aparecem os bloqueios registrados pela moderação e pelo painel.</p></CardHeader>
        <CardContent>{activeBans.length ? <div className="space-y-2">{activeBans.map((ban) => <div key={ban.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 p-3"><div><p className="font-semibold">@{ban.username} <Badge variant="outline" className="ml-2">{ban.scope}</Badge></p><p className="text-sm text-slate-600">{ban.reason}</p></div><p className="text-sm text-slate-500">{ban.expiresAt ? `até ${new Date(ban.expiresAt).toLocaleString('pt-BR')}` : 'permanente'}</p><Button size="sm" variant="outline" onClick={() => void handleUnban(ban.userId)}>Remover suspensão</Button></div>)}</div> : <p className="text-sm text-slate-500">Nenhuma suspensão ativa.</p>}</CardContent>
      </Card>
      <Card className="bg-white border-slate-200 text-slate-900 shadow-sm">
        <CardHeader><CardTitle>Denúncias para revisão</CardTitle><p className="text-sm text-slate-500">A IA classifica o conteúdo; a decisão administrativa fica registrada nesta fila.</p></CardHeader>
        <CardContent>{reports.length ? <div className="space-y-3">{reports.map((report) => <div key={report.id} className="rounded-lg border border-slate-200 p-3"><p className="font-semibold">@{report.reportedUsername}</p><p className="text-sm text-slate-600">Denunciado por @{report.reporterUsername} · {report.category || 'Outro'} · {report.targetType || 'USER'} {report.targetId ? `#${report.targetId}` : ''}</p><p className="mt-1 text-sm">{report.reason || 'Motivo não informado.'}</p><textarea value={reportResponses[report.id] || ''} onChange={(event) => setReportResponses((current) => ({ ...current, [report.id]: event.target.value }))} rows={2} placeholder="Mensagem de retorno para o usuário denunciado (opcional)" className="mt-3 w-full rounded border border-slate-300 px-3 py-2 text-sm" /><div className="mt-3 flex gap-2"><Button size="sm" variant="outline" onClick={() => void reviewReport(report.id, 'DISMISSED')}>Arquivar</Button><Button size="sm" onClick={() => void reviewReport(report.id, 'REVIEWED')}>Marcar revisada</Button></div></div>)}</div> : <p className="text-sm text-slate-500">Nenhuma denúncia pendente.</p>}</CardContent>
      </Card>
      <Card className="bg-white border-slate-200 text-slate-900 shadow-sm">
        <CardHeader><CardTitle>Comunidades da plataforma</CardTitle><p className="text-sm text-slate-500">O administrador da plataforma pode apagar uma comunidade ou transferir a propriedade.</p></CardHeader>
        <CardContent>
          {transferTarget && <div className="mb-4 rounded-lg border border-sky-200 bg-sky-50 p-4">
            <p className="font-semibold">Transferir propriedade de {transferTarget.name}</p>
            <p className="mt-1 text-sm text-slate-600">Somente membros aprovados aparecem nesta lista.</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <select value={selectedOwnerId} onChange={(event) => setSelectedOwnerId(event.target.value)} disabled={loadingTransferMembers} className="min-w-64 rounded border border-slate-300 bg-white px-3 py-2">
                <option value="">{loadingTransferMembers ? 'Carregando membros...' : 'Selecione o novo dono'}</option>
                {transferMembers.map((member) => <option key={member.userId} value={member.userId}>{member.name || member.username} (@{member.username})</option>)}
              </select>
              <Button type="button" onClick={() => void transferCommunityOwner()} disabled={loadingTransferMembers || !selectedOwnerId} className="bg-skillswap-teal text-white">Confirmar transferência</Button>
              <Button type="button" variant="outline" onClick={() => setTransferTarget(null)}>Cancelar</Button>
            </div>
          </div>}
          <div className="space-y-2">{communities.map((community) => <div key={community.communityId} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 p-3"><div><p className="font-semibold">{community.name}</p><p className="text-sm text-slate-600">{community.memberCount ?? 0} membros · {community.privateCommunity ? 'Privada' : 'Pública'}</p></div><div className="flex flex-wrap gap-2"><Button size="sm" variant="outline" onClick={() => void openOwnershipTransfer(community)}>Transferir dono</Button><Button size="sm" variant="destructive" onClick={async () => { if (!window.confirm(`Excluir a comunidade ${community.name}?`)) return; try { await AdminService.deleteCommunity(community.communityId); toast({ title: 'Comunidade excluída' }); await loadAdminData(); } catch { toast({ title: 'Erro', description: 'Não foi possível excluir a comunidade.', variant: 'destructive' }); } }}><Trash2 className="mr-1 h-4 w-4" />Excluir</Button></div></div>)}{!communities.length && <p className="text-sm text-slate-500">Nenhuma comunidade cadastrada.</p>}</div>
        </CardContent>
      </Card>
      <Card className="bg-white border-slate-200 text-slate-900 shadow-sm">
        <CardHeader><CardTitle className="flex items-center gap-2"><BriefcaseBusiness className="h-5 w-5 text-skillswap-teal" /> Projetos e oportunidades</CardTitle><p className="text-sm text-slate-500">Cadastre o que deve aparecer em Minha Jornada. O acesso continua gratuito.</p></CardHeader>
        <CardContent className="space-y-5">
          <form onSubmit={createOpportunity} className="grid gap-3 md:grid-cols-2">
            <input required value={opportunityForm.organizationName} onChange={(event) => setOpportunityForm({ ...opportunityForm, organizationName: event.target.value })} placeholder="Organização ou responsável" className="rounded border px-3 py-2" />
            <input required value={opportunityForm.title} onChange={(event) => setOpportunityForm({ ...opportunityForm, title: event.target.value })} placeholder="Título" className="rounded border px-3 py-2" />
            <textarea required value={opportunityForm.description} onChange={(event) => setOpportunityForm({ ...opportunityForm, description: event.target.value })} placeholder="Descrição" className="min-h-24 rounded border px-3 py-2 md:col-span-2" />
            <input required value={opportunityForm.requiredSkills} onChange={(event) => setOpportunityForm({ ...opportunityForm, requiredSkills: event.target.value })} placeholder="Habilidades (separadas por vírgula)" className="rounded border px-3 py-2" />
            <select value={opportunityForm.participationMode} onChange={(event) => setOpportunityForm({ ...opportunityForm, participationMode: event.target.value })} className="rounded border px-3 py-2"><option value="REMOTE">Remoto</option><option value="HYBRID">Semi-presencial</option><option value="PRESENTIAL">Presencial</option></select>
            <input value={opportunityForm.location} onChange={(event) => setOpportunityForm({ ...opportunityForm, location: event.target.value })} placeholder="Local (opcional)" className="rounded border px-3 py-2" />
            <input type="url" value={opportunityForm.contactUrl} onChange={(event) => setOpportunityForm({ ...opportunityForm, contactUrl: event.target.value })} placeholder="Link de contato WhatsApp ou e-mail (opcional)" className="rounded border px-3 py-2" />
            <Button type="submit" className="w-fit bg-skillswap-teal text-white">Publicar oportunidade</Button>
          </form>
          <div className="space-y-2 border-t border-slate-200 pt-4">{opportunities.map((opportunity) => <div key={opportunity.opportunityId} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 p-3"><div><p className="font-semibold">{opportunity.title}</p><p className="text-sm text-slate-600">{opportunity.organizationName} · {opportunity.participationMode === 'HYBRID' ? 'Semi-presencial' : opportunity.participationMode === 'PRESENTIAL' ? 'Presencial' : 'Remoto'}</p></div><Button type="button" size="sm" variant="outline" className="text-red-600" onClick={() => void deleteOpportunity(opportunity.opportunityId)}><Trash2 className="mr-1 h-4 w-4" /> Remover</Button></div>)}{!opportunities.length && <p className="text-sm text-slate-500">Nenhuma oportunidade cadastrada.</p>}</div>
        </CardContent>
      </Card>
    </div>
  );
};

const StatCard = ({ title, value, icon }: { title: string; value: number | string; icon: React.ReactNode }) => (
  <Card className="bg-white border-slate-200 text-slate-900 shadow-sm"><CardHeader className="flex flex-row items-center justify-between pb-2"><CardTitle className="text-sm font-medium text-slate-500">{title}</CardTitle><div className="text-skillswap-teal">{icon}</div></CardHeader><CardContent><div className="text-2xl font-bold">{value}</div></CardContent></Card>
);

export default AdminConsole;
