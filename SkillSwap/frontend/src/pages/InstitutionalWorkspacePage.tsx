import { FormEvent, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { BarChart3, Building2, CreditCard, FilePlus2, Loader2, MailPlus, ShieldCheck, Users, BriefcaseBusiness, CheckCircle2, UserMinus } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { InstitutionalService } from '@/services/api';

type Organization = { organizationId: string; name: string; legalName?: string; type: string; country?: string; website?: string; planCode: string; subscriptionStatus: string; seatLimit: number; seatUsage: number; billingCurrency: string; monthlyPriceCents: number; membershipRole: string };
type Dashboard = { organization: Organization; members: number; activeMembers: number; opportunities: number; openOpportunities: number; applications: number; activePlans: number; evidences: number; applicationsByStatus: Record<string, number>; recentOpportunities: Opportunity[] };
type Member = { membershipId: string; userId: string; name: string; username: string; email: string; role: string; status: string; department?: string; createdAt: string };
type Opportunity = { opportunityId: string; organizationId?: string; organizationName: string; title: string; description: string; requiredSkills: string; remote: boolean; accessibleSupport: boolean; location?: string; status: string; createdAt: string; applicationCount: number };
type Application = { applicationId: string; opportunityId: string; opportunityTitle: string; applicantId: string; applicantName: string; applicantUsername: string; applicantEmail: string; status: string; message?: string; createdAt: string; updatedAt: string };
type Plan = { code: string; name: string; defaultSeats: number; monthlyUsdCents: number; monthlyEurCents: number; monthlyBrlCents: number; description: string };
type Subscription = { planCode: string; subscriptionStatus: string; seatLimit: number; billingCurrency: string; monthlyPriceCents: number; billingEmail?: string; stripeConfigured: boolean; stripeCheckoutSessionId?: string };

const formatMoney = (cents: number, currency: string) => new Intl.NumberFormat(currency === 'BRL' ? 'pt-BR' : currency === 'EUR' ? 'de-DE' : 'en-US', { style: 'currency', currency }).format(cents / 100);

const initialOrganization = { name: '', type: 'COMPANY', legalName: '', country: '', website: '', billingEmail: '' };
const initialOpportunity = { title: '', description: '', requiredSkills: '', location: '', remote: true, accessibleSupport: true, status: 'OPEN' };
const initialMember = { email: '', role: 'MEMBER', department: '' };
const roleLabels: Record<string, string> = { OWNER: 'Proprietário', ADMIN: 'Administrador', MANAGER: 'Gestor', ANALYST: 'Analista', MEMBER: 'Membro' };
const typeLabels: Record<string, string> = { UNIVERSITY: 'Universidade', COMPANY: 'Empresa', GOVERNMENT: 'Governo', NGO: 'ONG', TRAINING_PROVIDER: 'Capacitação', OTHER: 'Outra instituição' };

const InstitutionalWorkspacePage = () => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [organizationId, setOrganizationId] = useState('');
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [organizationForm, setOrganizationForm] = useState(initialOrganization);
  const [opportunityForm, setOpportunityForm] = useState(initialOpportunity);
  const [memberForm, setMemberForm] = useState(initialMember);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  const activeOrganization = useMemo(() => organizations.find((item) => item.organizationId === organizationId), [organizations, organizationId]);
  const canManage = ['OWNER', 'ADMIN', 'MANAGER'].includes(activeOrganization?.membershipRole || '');
  const canBill = ['OWNER', 'ADMIN'].includes(activeOrganization?.membershipRole || '');

  const loadWorkspace = async (selectedId?: string) => {
    setLoading(true);
    try {
      const [organizationResponse, catalogResponse] = await Promise.all([InstitutionalService.getOrganizations(), InstitutionalService.getCatalog()]);
      const nextOrganizations = (organizationResponse.data || []) as Organization[];
      setOrganizations(nextOrganizations);
      setPlans((catalogResponse.data || []) as Plan[]);
      const nextId = selectedId || organizationId || nextOrganizations[0]?.organizationId || '';
      setOrganizationId(nextId);
      if (!nextId) return;
      const [dashboardResponse, memberResponse, opportunityResponse, applicationResponse, subscriptionResponse] = await Promise.all([
        InstitutionalService.getDashboard(nextId), InstitutionalService.getMembers(nextId), InstitutionalService.getOpportunities(nextId), InstitutionalService.getApplications(nextId), InstitutionalService.getSubscription(nextId),
      ]);
      setDashboard(dashboardResponse.data as Dashboard);
      setMembers((memberResponse.data || []) as Member[]);
      setOpportunities((opportunityResponse.data || []) as Opportunity[]);
      setApplications((applicationResponse.data || []) as Application[]);
      setSubscription(subscriptionResponse.data as Subscription);
    } catch {
      toast.error('Não foi possível carregar o workspace institucional.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadWorkspace(); }, []);

  const createOrganization = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true);
    try { const response = await InstitutionalService.createOrganization(organizationForm); setOrganizationForm(initialOrganization); toast.success('Organização criada.'); await loadWorkspace(response.data.organizationId); }
    catch { toast.error('Não foi possível criar a organização.'); } finally { setSaving(false); }
  };

  const addMember = async (event: FormEvent) => {
    event.preventDefault(); if (!organizationId) return; setSaving(true);
    try { await InstitutionalService.addMember(organizationId, memberForm); setMemberForm(initialMember); toast.success('Membro vinculado à organização.'); await loadWorkspace(organizationId); }
    catch { toast.error('O usuário precisa existir e o plano precisa ter assento disponível.'); } finally { setSaving(false); }
  };

  const createOpportunity = async (event: FormEvent) => {
    event.preventDefault(); if (!organizationId) return; setSaving(true);
    try { await InstitutionalService.createOpportunity(organizationId, opportunityForm); setOpportunityForm(initialOpportunity); toast.success('Oportunidade publicada no workspace.'); await loadWorkspace(organizationId); }
    catch { toast.error('Não foi possível publicar a oportunidade.'); } finally { setSaving(false); }
  };

  const updateSubscription = async (event: FormEvent) => {
    event.preventDefault(); if (!organizationId || !subscription) return; setSaving(true);
    try { await InstitutionalService.updateSubscription(organizationId, { planCode: subscription.planCode, seatLimit: subscription.seatLimit, currency: subscription.billingCurrency, billingEmail: subscription.billingEmail }); toast.success('Plano institucional atualizado.'); await loadWorkspace(organizationId); }
    catch { toast.error('Não foi possível atualizar o plano ou o limite de assentos.'); } finally { setSaving(false); }
  };

  const checkout = async () => {
    if (!organizationId || !subscription) return; setSaving(true);
    try { const response = await InstitutionalService.createBillingCheckout(organizationId, { planCode: subscription.planCode, seatLimit: subscription.seatLimit, currency: subscription.billingCurrency, billingEmail: subscription.billingEmail }); window.location.assign(response.data.checkoutUrl); }
    catch { toast.error('Configure STRIPE_SECRET_KEY para iniciar o checkout institucional.'); } finally { setSaving(false); }
  };

  const removeMember = async (member: Member) => {
    if (!organizationId || !window.confirm(`Suspender o acesso de ${member.name}?`)) return;
    try { await InstitutionalService.removeMember(organizationId, member.membershipId); toast.success('Acesso institucional suspenso.'); await loadWorkspace(organizationId); }
    catch { toast.error('Não foi possível suspender o membro.'); }
  };

  const setOpportunityStatus = async (opportunity: Opportunity, status: string) => {
    if (!organizationId) return;
    try { await InstitutionalService.updateOpportunityStatus(organizationId, opportunity.opportunityId, status); await loadWorkspace(organizationId); }
    catch { toast.error('Não foi possível atualizar a oportunidade.'); }
  };

  const setApplicationStatus = async (application: Application, status: string) => {
    if (!organizationId) return;
    try { await InstitutionalService.updateApplicationStatus(organizationId, application.applicationId, status); await loadWorkspace(organizationId); }
    catch { toast.error('Não foi possível atualizar a candidatura.'); }
  };

  if (loading && !organizations.length) return <div className="flex min-h-[60vh] items-center justify-center"><Loader2 className="h-8 w-8 animate-spin text-skillswap-teal" /></div>;

  return <div className="mx-auto max-w-7xl space-y-6 p-2 sm:p-4">
    <section className="overflow-hidden rounded-3xl border border-skillswap-teal/30 bg-gradient-to-br from-skillswap-teal/20 via-card to-card p-6 shadow-sm sm:p-8">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between"><div className="max-w-3xl"><div className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-skillswap-teal"><Building2 className="h-4 w-4" /> Institutional Workspace</div><h1 className="text-3xl font-black tracking-tight sm:text-4xl">Gestão de talentos e oportunidades</h1><p className="mt-3 text-muted-foreground">Universidades, empresas, governos e ONGs podem organizar equipes, publicar desafios, acompanhar candidaturas e medir a jornada de aprendizagem.</p></div><div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-3xl bg-skillswap-teal text-white shadow-lg"><BarChart3 className="h-12 w-12" /></div></div>
    </section>

    {organizations.length > 0 && <div className="flex flex-col gap-3 rounded-2xl border bg-card p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Organização ativa</p><p className="font-bold">{activeOrganization?.name} <Badge variant="outline" className="ml-2">{roleLabels[activeOrganization?.membershipRole || ''] || activeOrganization?.membershipRole}</Badge></p></div><select className="h-10 rounded-md border border-input bg-background px-3 text-sm" value={organizationId} onChange={(event) => { setOrganizationId(event.target.value); void loadWorkspace(event.target.value); }} aria-label="Selecionar organização">{organizations.map((item) => <option key={item.organizationId} value={item.organizationId}>{item.name}</option>)}</select></div>}

    {organizations.length === 0 ? <Card className="rounded-3xl"><CardHeader><CardTitle>Crie o primeiro workspace institucional</CardTitle></CardHeader><CardContent><OrganizationForm value={organizationForm} onChange={setOrganizationForm} onSubmit={createOrganization} saving={saving} /></CardContent></Card> : <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><Metric icon={Users} label="Membros ativos" value={`${dashboard?.activeMembers ?? 0}/${dashboard?.organization.seatLimit ?? 0}`} helper="assentos do plano" /><Metric icon={BriefcaseBusiness} label="Oportunidades abertas" value={String(dashboard?.openOpportunities ?? 0)} helper="desafios em circulação" /><Metric icon={CheckCircle2} label="Candidaturas" value={String(dashboard?.applications ?? 0)} helper="candidaturas recebidas" /><Metric icon={BarChart3} label="Jornadas" value={String(dashboard?.activePlans ?? 0)} helper="planos de evolução da equipe" /></div>
      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card className="rounded-3xl"><CardHeader><CardTitle className="flex items-center gap-2"><Users className="h-5 w-5 text-skillswap-teal" /> Equipe e permissões</CardTitle><p className="text-sm text-muted-foreground">Vincule usuários existentes e controle o papel de cada pessoa dentro da organização.</p></CardHeader><CardContent className="space-y-4">{canManage && <form onSubmit={addMember} className="grid gap-3 rounded-2xl border border-dashed p-4 sm:grid-cols-[1fr_150px_150px_auto]"><Input required type="email" value={memberForm.email} onChange={(event) => setMemberForm({ ...memberForm, email: event.target.value })} placeholder="email@instituicao.org" aria-label="E-mail do membro" /><select className="h-10 rounded-md border border-input bg-background px-3 text-sm" value={memberForm.role} onChange={(event) => setMemberForm({ ...memberForm, role: event.target.value })} aria-label="Papel do membro"><option value="MEMBER">Membro</option><option value="ANALYST">Analista</option><option value="MANAGER">Gestor</option><option value="ADMIN">Administrador</option></select><Input value={memberForm.department} onChange={(event) => setMemberForm({ ...memberForm, department: event.target.value })} placeholder="Departamento" aria-label="Departamento" /><Button type="submit" disabled={saving} className="gap-2"><MailPlus className="h-4 w-4" /> Vincular</Button></form>}{members.map((member) => <div key={member.membershipId} className="flex flex-col gap-3 rounded-2xl border p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-bold">{member.name}</p><p className="text-sm text-muted-foreground">{member.email}{member.department ? ` · ${member.department}` : ''}</p></div><div className="flex items-center gap-2"><Badge variant="outline">{roleLabels[member.role] || member.role}</Badge>{canManage && member.role !== 'OWNER' && <Button variant="ghost" size="icon" onClick={() => void removeMember(member)} aria-label={`Suspender ${member.name}`}><UserMinus className="h-4 w-4 text-destructive" /></Button>}</div></div>)}{members.length === 0 && <p className="text-sm text-muted-foreground">Nenhum membro ativo.</p>}</CardContent></Card>
        <Card className="rounded-3xl"><CardHeader><CardTitle className="flex items-center gap-2"><CreditCard className="h-5 w-5 text-skillswap-teal" /> Plano e licenças</CardTitle><p className="text-sm text-muted-foreground">Controle assentos, moeda e assinatura institucional.</p></CardHeader><CardContent>{subscription ? <form onSubmit={updateSubscription} className="space-y-4"><div className="grid gap-3 sm:grid-cols-2"><label className="space-y-2 text-sm font-semibold">Plano<select className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={!canBill} value={subscription.planCode} onChange={(event) => setSubscription({ ...subscription, planCode: event.target.value })}>{plans.map((plan) => <option key={plan.code} value={plan.code}>{plan.name} · {plan.defaultSeats} assentos</option>)}</select></label><label className="space-y-2 text-sm font-semibold">Assentos<input className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={!canBill} type="number" min={1} value={subscription.seatLimit} onChange={(event) => setSubscription({ ...subscription, seatLimit: Number(event.target.value) })} /></label></div><label className="block space-y-2 text-sm font-semibold">Moeda<select className="mt-1 h-10 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={!canBill} value={subscription.billingCurrency} onChange={(event) => setSubscription({ ...subscription, billingCurrency: event.target.value })}><option value="BRL">BRL · Brasil</option><option value="EUR">EUR · Europa</option><option value="USD">USD · EUA e mercados dolarizados</option></select></label><Input disabled={!canBill} type="email" value={subscription.billingEmail || ''} onChange={(event) => setSubscription({ ...subscription, billingEmail: event.target.value })} placeholder="financeiro@instituicao.org" aria-label="E-mail de cobrança" /><div className="rounded-2xl bg-muted/50 p-4 text-sm"><p className="font-bold">Status: {subscription.subscriptionStatus}</p><p className="text-muted-foreground">Valor mensal: {formatMoney(subscription.monthlyPriceCents, subscription.billingCurrency)} · {subscription.seatLimit} assentos</p></div>{canBill && <div className="flex flex-wrap gap-2"><Button type="submit" disabled={saving}>Salvar plano</Button><Button type="button" variant="outline" disabled={saving || !subscription.stripeConfigured || subscription.planCode === 'TRIAL'} onClick={() => void checkout()}>Assinar com Stripe</Button></div>}</form> : <p className="text-sm text-muted-foreground">Carregando assinatura...</p>}</CardContent></Card>
      </div>
      <Card className="rounded-3xl"><CardHeader><CardTitle className="flex items-center gap-2"><FilePlus2 className="h-5 w-5 text-skillswap-teal" /> Oportunidades institucionais</CardTitle><p className="text-sm text-muted-foreground">Publique desafios e conecte candidatos com evidências de habilidade.</p></CardHeader><CardContent className="space-y-5">{canManage && <form onSubmit={createOpportunity} className="grid gap-3 rounded-2xl border border-dashed p-4 md:grid-cols-2"><Input required value={opportunityForm.title} onChange={(event) => setOpportunityForm({ ...opportunityForm, title: event.target.value })} placeholder="Título da oportunidade" aria-label="Título da oportunidade" /><Input required value={opportunityForm.requiredSkills} onChange={(event) => setOpportunityForm({ ...opportunityForm, requiredSkills: event.target.value })} placeholder="Habilidades separadas por vírgula" aria-label="Habilidades necessárias" /><Textarea required className="md:col-span-2" value={opportunityForm.description} onChange={(event) => setOpportunityForm({ ...opportunityForm, description: event.target.value })} placeholder="Descreva o desafio, o resultado esperado e os critérios de participação" aria-label="Descrição da oportunidade" /><div className="flex gap-3"><Input value={opportunityForm.location} onChange={(event) => setOpportunityForm({ ...opportunityForm, location: event.target.value })} placeholder="Local ou região" aria-label="Local" /><Button type="submit" disabled={saving} className="gap-2"><FilePlus2 className="h-4 w-4" /> Publicar</Button></div></form>}{opportunities.map((opportunity) => <div key={opportunity.opportunityId} className="flex flex-col gap-3 rounded-2xl border p-4 lg:flex-row lg:items-center lg:justify-between"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="font-bold">{opportunity.title}</h3><Badge variant={opportunity.status === 'OPEN' ? 'default' : 'outline'}>{opportunity.status}</Badge><span className="text-xs text-muted-foreground">{opportunity.applicationCount} candidaturas</span></div><p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{opportunity.description}</p><p className="mt-2 text-xs font-semibold text-skillswap-teal">{opportunity.requiredSkills}</p></div>{canManage && <select className="h-9 rounded-md border border-input bg-background px-3 text-sm" value={opportunity.status} onChange={(event) => void setOpportunityStatus(opportunity, event.target.value)} aria-label={`Status de ${opportunity.title}`}><option value="DRAFT">Rascunho</option><option value="OPEN">Aberta</option><option value="CLOSED">Fechada</option><option value="ARCHIVED">Arquivada</option></select>}</div>)}{opportunities.length === 0 && <p className="text-sm text-muted-foreground">Publique a primeira oportunidade institucional.</p>}</CardContent></Card>
      <Card className="rounded-3xl"><CardHeader><CardTitle className="flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-skillswap-teal" /> Candidaturas e relatório agregado</CardTitle><p className="text-sm text-muted-foreground">Acompanhe o funil sem expor dados fora da organização.</p></CardHeader><CardContent className="space-y-3"><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">{Object.entries(dashboard?.applicationsByStatus || {}).map(([status, value]) => <div key={status} className="rounded-2xl bg-muted/50 p-3"><p className="text-xs font-bold uppercase text-muted-foreground">{status}</p><p className="text-2xl font-black">{value}</p></div>)}</div>{applications.map((application) => <div key={application.applicationId} className="flex flex-col gap-3 rounded-2xl border p-4 lg:flex-row lg:items-center lg:justify-between"><div><p className="font-bold">{application.applicantName} · {application.opportunityTitle}</p><p className="text-sm text-muted-foreground">{application.applicantEmail}{application.message ? ` · ${application.message}` : ''}</p></div>{canManage && <select className="h-9 rounded-md border border-input bg-background px-3 text-sm" value={application.status} onChange={(event) => void setApplicationStatus(application, event.target.value)} aria-label={`Status de ${application.applicantName}`}><option value="SUBMITTED">Recebida</option><option value="REVIEWING">Em análise</option><option value="ACCEPTED">Aceita</option><option value="REJECTED">Rejeitada</option><option value="WITHDRAWN">Retirada</option></select>}</div>)}{applications.length === 0 && <p className="text-sm text-muted-foreground">As candidaturas aparecerão aqui quando usuários se candidatarem.</p>}</CardContent></Card>
    </>}
  </div>;
};

const OrganizationForm = ({ value, onChange, onSubmit, saving }: { value: typeof initialOrganization; onChange: (value: typeof initialOrganization) => void; onSubmit: (event: FormEvent) => void; saving: boolean }) => <form onSubmit={onSubmit} className="grid gap-4 md:grid-cols-2"><Input required value={value.name} onChange={(event) => onChange({ ...value, name: event.target.value })} placeholder="Nome da universidade, empresa ou instituição" aria-label="Nome da organização" /><select className="h-10 rounded-md border border-input bg-background px-3 text-sm" value={value.type} onChange={(event) => onChange({ ...value, type: event.target.value })} aria-label="Tipo da organização"><option value="UNIVERSITY">Universidade</option><option value="COMPANY">Empresa</option><option value="GOVERNMENT">Governo</option><option value="NGO">ONG</option><option value="TRAINING_PROVIDER">Capacitação profissional</option><option value="OTHER">Outra</option></select><Input value={value.legalName} onChange={(event) => onChange({ ...value, legalName: event.target.value })} placeholder="Razão social (opcional)" aria-label="Razão social" /><Input value={value.country} onChange={(event) => onChange({ ...value, country: event.target.value })} placeholder="País" aria-label="País" /><Input type="url" value={value.website} onChange={(event) => onChange({ ...value, website: event.target.value })} placeholder="https://instituicao.org" aria-label="Website" /><Input type="email" value={value.billingEmail} onChange={(event) => onChange({ ...value, billingEmail: event.target.value })} placeholder="financeiro@instituicao.org" aria-label="E-mail de cobrança" /><Button type="submit" disabled={saving} className="w-fit gap-2"><Building2 className="h-4 w-4" /> Criar workspace</Button></form>;
const Metric = ({ icon: Icon, label, value, helper }: { icon: typeof Users; label: string; value: string; helper: string }) => <Card className="rounded-2xl"><CardContent className="p-5"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{label}</p><p className="mt-2 text-3xl font-black">{value}</p></div><Icon className="h-5 w-5 text-skillswap-teal" /></div><p className="mt-2 text-xs text-muted-foreground">{helper}</p></CardContent></Card>;

export default InstitutionalWorkspacePage;
