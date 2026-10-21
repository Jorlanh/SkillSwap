import { FormEvent, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import {
  ArrowRight,
  BriefcaseBusiness,
  CheckCircle2,
  ClipboardCheck,
  ExternalLink,
  FileCheck2,
  Flag,
  Loader2,
  Plus,
  Rocket,
  Send,
  Sparkles,
  Target,
  Timer,
  Trophy,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { AccessService, OpportunityOsService } from '@/services/api';

type Step = {
  stepId: number;
  stepOrder: number;
  title: string;
  description: string;
  kind: string;
  skillFocus?: string;
  estimatedHours: number;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  evidenceId?: string;
};

type Plan = {
  planId: string;
  goal: string;
  targetRole?: string;
  constraints?: string;
  weeklyHours: number;
  status: string;
  progressPercent: number;
  steps: Step[];
};

type Dashboard = {
  plan: Plan | null;
  evidenceCount: number;
  applicationCount: number;
  nextAction: string;
};

type Evidence = {
  evidenceId: string;
  title: string;
  skillFocus: string;
  description: string;
  artifactUrl?: string;
  evidenceType: string;
  verificationStatus: string;
  planStepId?: number;
};

type Opportunity = {
  opportunityId: string;
  organizationName: string;
  title: string;
  description: string;
  requiredSkills: string;
  remote: boolean;
  accessibleSupport: boolean;
  location?: string;
};

type Application = { opportunityId: string; status: string };

const initialPlanForm = { goal: '', targetRole: '', constraints: '', weeklyHours: 5 };
const initialEvidenceForm = { title: '', skillFocus: '', description: '', artifactUrl: '', evidenceType: 'PROJECT', planStepId: '', opportunityId: '' };

const statusLabel: Record<Step['status'], string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluída',
};

const statusColor: Record<Step['status'], string> = {
  TODO: 'bg-muted text-muted-foreground',
  IN_PROGRESS: 'bg-skillswap-light-blue text-foreground',
  DONE: 'bg-skillswap-teal text-white',
};

const OpportunityOsPage = () => {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [planForm, setPlanForm] = useState(initialPlanForm);
  const [evidenceForm, setEvidenceForm] = useState(initialEvidenceForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const appliedIds = useMemo(() => new Set(applications.map((item) => item.opportunityId)), [applications]);

  const load = async () => {
    setLoading(true);
    try {
      const [dashboardResponse, evidenceResponse, applicationsResponse, opportunitiesResponse] = await Promise.all([
        OpportunityOsService.getDashboard(),
        OpportunityOsService.getEvidence(),
        OpportunityOsService.getApplications(),
        AccessService.getOpportunities(),
      ]);
      setDashboard(dashboardResponse.data);
      setEvidence(evidenceResponse.data || []);
      setApplications(applicationsResponse.data || []);
      setOpportunities(opportunitiesResponse.data || []);
    } catch {
      toast.error('Não foi possível carregar sua jornada agora.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const createPlan = async (event: FormEvent) => {
    event.preventDefault();
    if (!planForm.goal.trim()) {
      toast.error('Informe o objetivo da sua jornada.');
      return;
    }
    setSaving(true);
    try {
      await OpportunityOsService.createPlan(planForm);
      toast.success('Plano criado. Agora transforme intenção em evidência.');
      setPlanForm(initialPlanForm);
      await load();
    } catch {
      toast.error('Não foi possível criar o plano.');
    } finally {
      setSaving(false);
    }
  };

  const advanceStep = async (step: Step) => {
    const nextStatus = step.status === 'TODO' ? 'IN_PROGRESS' : step.status === 'IN_PROGRESS' ? 'DONE' : 'TODO';
    try {
      await OpportunityOsService.updateStep(step.stepId, nextStatus);
      toast.success(nextStatus === 'DONE' ? 'Etapa concluída.' : 'Etapa atualizada.');
      await load();
    } catch {
      toast.error('Não foi possível atualizar a etapa.');
    }
  };

  const createEvidence = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      await OpportunityOsService.createEvidence({
        ...evidenceForm,
        planStepId: evidenceForm.planStepId ? Number(evidenceForm.planStepId) : null,
        opportunityId: evidenceForm.opportunityId || null,
      });
      toast.success('Evidência adicionada ao seu portfólio.');
      setEvidenceForm(initialEvidenceForm);
      await load();
    } catch {
      toast.error('Preencha título, habilidade e resultado antes de salvar.');
    } finally {
      setSaving(false);
    }
  };

  const applyToOpportunity = async (opportunity: Opportunity) => {
    try {
      await OpportunityOsService.apply(opportunity.opportunityId, `Tenho interesse em participar do projeto ${opportunity.title}.`);
      toast.success('Candidatura enviada.');
      await load();
    } catch {
      toast.error('Você já se candidatou ou a oportunidade não está disponível.');
    }
  };

  if (loading && !dashboard) {
    return <div className="flex min-h-[60vh] items-center justify-center"><Loader2 className="h-8 w-8 animate-spin text-skillswap-teal" /></div>;
  }

  const plan = dashboard?.plan;

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-2 sm:p-4">
      <section className="overflow-hidden rounded-3xl border border-border bg-gradient-to-br from-skillswap-teal/25 via-card to-card p-6 shadow-sm sm:p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="max-w-3xl">
            <div className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-skillswap-teal"><Sparkles className="h-4 w-4" /> SkillSwap Access</div>
            <h1 className="text-3xl font-black tracking-tight sm:text-4xl">Minha jornada de oportunidades</h1>
            <p className="mt-3 text-muted-foreground">Transforme uma meta em aprendizado, projeto real, evidência verificável e candidatura. Cada etapa deixa um próximo passo claro.</p>
          </div>
          <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-3xl bg-skillswap-teal text-white shadow-lg"><Rocket className="h-12 w-12" /></div>
        </div>
      </section>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Metric icon={Target} label="Progresso do plano" value={`${plan?.progressPercent ?? 0}%`} helper={dashboard?.nextAction || 'Comece definindo sua meta'} />
        <Metric icon={FileCheck2} label="Evidências" value={String(dashboard?.evidenceCount ?? 0)} helper="provas do que você já fez" />
        <Metric icon={Send} label="Candidaturas" value={String(dashboard?.applicationCount ?? 0)} helper="oportunidades em movimento" />
        <Metric icon={Timer} label="Ritmo semanal" value={plan ? `${plan.weeklyHours}h` : '—'} helper="tempo reservado para avançar" />
      </div>

      {!plan ? (
        <Card className="rounded-3xl border-skillswap-teal/30">
          <CardHeader><CardTitle className="flex items-center gap-2"><Flag className="h-5 w-5 text-skillswap-teal" /> Crie seu primeiro plano</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={createPlan} className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2 md:col-span-2"><label htmlFor="goal" className="text-sm font-semibold">Qual resultado você deseja alcançar?</label><Input id="goal" value={planForm.goal} onChange={(event) => setPlanForm({ ...planForm, goal: event.target.value })} placeholder="Ex.: conseguir meu primeiro projeto como desenvolvedor web" /></div>
              <div className="space-y-2"><label htmlFor="targetRole" className="text-sm font-semibold">Função ou direção desejada</label><Input id="targetRole" value={planForm.targetRole} onChange={(event) => setPlanForm({ ...planForm, targetRole: event.target.value })} placeholder="Ex.: desenvolvedor frontend" /></div>
              <div className="space-y-2"><label htmlFor="weeklyHours" className="text-sm font-semibold">Horas por semana</label><Input id="weeklyHours" type="number" min={1} max={40} value={planForm.weeklyHours} onChange={(event) => setPlanForm({ ...planForm, weeklyHours: Number(event.target.value) })} /></div>
              <div className="space-y-2 md:col-span-2"><label htmlFor="constraints" className="text-sm font-semibold">Limitações ou preferências</label><Textarea id="constraints" value={planForm.constraints} onChange={(event) => setPlanForm({ ...planForm, constraints: event.target.value })} placeholder="Ex.: preciso de atividades remotas, tenho apenas noites livres e prefiro materiais em linguagem simples" /></div>
              <Button type="submit" disabled={saving} className="w-fit gap-2 bg-skillswap-teal hover:bg-skillswap-teal/90">{saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />} Gerar plano de evolução</Button>
            </form>
          </CardContent>
        </Card>
      ) : (
        <Card className="rounded-3xl">
          <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><CardTitle className="flex items-center gap-2"><ClipboardCheck className="h-5 w-5 text-skillswap-teal" /> Plano de evolução</CardTitle><p className="mt-2 text-sm text-muted-foreground">{plan.goal}{plan.targetRole ? ` · direção: ${plan.targetRole}` : ''}</p></div><Badge className="w-fit bg-skillswap-teal text-white">{plan.progressPercent}% concluído</Badge></CardHeader>
          <CardContent className="space-y-3">
            {plan.steps.map((step) => <StepCard key={step.stepId} step={step} onAdvance={() => advanceStep(step)} />)}
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card className="rounded-3xl">
          <CardHeader><CardTitle className="flex items-center gap-2"><FileCheck2 className="h-5 w-5 text-skillswap-teal" /> Portfólio de evidências</CardTitle><p className="text-sm text-muted-foreground">Não diga apenas que sabe. Registre o que você entregou, o resultado e onde pode ser conferido.</p></CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={createEvidence} className="grid gap-3">
              <Input required value={evidenceForm.title} onChange={(event) => setEvidenceForm({ ...evidenceForm, title: event.target.value })} placeholder="Título do projeto ou entrega" aria-label="Título da evidência" />
              <Input required value={evidenceForm.skillFocus} onChange={(event) => setEvidenceForm({ ...evidenceForm, skillFocus: event.target.value })} placeholder="Habilidade demonstrada" aria-label="Habilidade demonstrada" />
              <Textarea required value={evidenceForm.description} onChange={(event) => setEvidenceForm({ ...evidenceForm, description: event.target.value })} placeholder="O que você fez, para quem e qual foi o resultado?" aria-label="Descrição da evidência" />
              <div className="grid gap-3 sm:grid-cols-2"><Input value={evidenceForm.artifactUrl} onChange={(event) => setEvidenceForm({ ...evidenceForm, artifactUrl: event.target.value })} placeholder="Link do artefato (opcional)" aria-label="Link do artefato" /><select className="h-10 rounded-md border border-input bg-background px-3 text-sm" value={evidenceForm.planStepId} onChange={(event) => setEvidenceForm({ ...evidenceForm, planStepId: event.target.value })} aria-label="Etapa relacionada"><option value="">Relacionar a uma etapa</option>{plan?.steps.map((step) => <option key={step.stepId} value={step.stepId}>{step.stepOrder}. {step.title}</option>)}</select></div>
              <Button type="submit" disabled={saving} variant="outline" className="w-fit gap-2"><Plus className="h-4 w-4" /> Adicionar evidência</Button>
            </form>
            <div className="space-y-3 border-t pt-4">
              {evidence.length === 0 ? <p className="text-sm text-muted-foreground">Sua primeira evidência pode ser uma tela, texto, código, vídeo, apresentação ou resultado de um projeto.</p> : evidence.map((item) => <EvidenceCard key={item.evidenceId} item={item} onReview={async () => { await OpportunityOsService.requestEvidenceReview(item.evidenceId); await load(); toast.success('Revisão solicitada.'); }} />)}
            </div>
          </CardContent>
        </Card>

        <Card className="rounded-3xl">
          <CardHeader><CardTitle className="flex items-center gap-2"><BriefcaseBusiness className="h-5 w-5 text-skillswap-teal" /> Projetos e oportunidades</CardTitle><p className="text-sm text-muted-foreground">Candidate-se a desafios que podem gerar prática, colaboração e uma nova evidência.</p></CardHeader>
          <CardContent className="space-y-3">
            {opportunities.map((opportunity) => <OpportunityCard key={opportunity.opportunityId} opportunity={opportunity} applied={appliedIds.has(opportunity.opportunityId)} onApply={() => applyToOpportunity(opportunity)} />)}
            {opportunities.length === 0 && <p className="text-sm text-muted-foreground">Ainda não há oportunidades publicadas.</p>}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

const Metric = ({ icon: Icon, label, value, helper }: { icon: typeof Target; label: string; value: string; helper: string }) => <Card className="rounded-2xl"><CardContent className="p-5"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{label}</p><p className="mt-2 text-3xl font-black">{value}</p></div><Icon className="h-5 w-5 text-skillswap-teal" /></div><p className="mt-2 line-clamp-2 text-xs text-muted-foreground">{helper}</p></CardContent></Card>;

const StepCard = ({ step, onAdvance }: { step: Step; onAdvance: () => void }) => <div className="flex flex-col gap-3 rounded-2xl border bg-background/60 p-4 sm:flex-row sm:items-center"><div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${step.status === 'DONE' ? 'bg-skillswap-teal text-white' : 'bg-muted text-muted-foreground'}`}>{step.status === 'DONE' ? <CheckCircle2 className="h-5 w-5" /> : <span className="font-black">{step.stepOrder}</span>}</div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h3 className="font-bold">{step.title}</h3><Badge className={statusColor[step.status]}>{statusLabel[step.status]}</Badge><span className="text-xs text-muted-foreground">{step.estimatedHours}h</span></div><p className="mt-1 text-sm text-muted-foreground">{step.description}</p>{step.skillFocus && <p className="mt-2 text-xs font-semibold text-skillswap-teal">Foco: {step.skillFocus}</p>}</div><Button variant={step.status === 'DONE' ? 'outline' : 'default'} size="sm" onClick={onAdvance} className="gap-2 sm:shrink-0">{step.status === 'TODO' ? 'Começar' : step.status === 'IN_PROGRESS' ? 'Concluir' : 'Reabrir'} <ArrowRight className="h-4 w-4" /></Button></div>;

const EvidenceCard = ({ item, onReview }: { item: Evidence; onReview: () => Promise<void> }) => <div className="rounded-2xl border p-4"><div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">{item.title}</h3><p className="text-xs font-semibold text-skillswap-teal">{item.skillFocus}</p></div><Badge variant="outline">{item.verificationStatus === 'SELF_REPORTED' ? 'Autodeclarada' : item.verificationStatus === 'REVIEW_REQUESTED' ? 'Em revisão' : item.verificationStatus}</Badge></div><p className="mt-2 text-sm text-muted-foreground">{item.description}</p><div className="mt-3 flex flex-wrap items-center gap-2">{item.artifactUrl && <a href={item.artifactUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-xs font-semibold text-skillswap-teal hover:underline">Abrir artefato <ExternalLink className="h-3 w-3" /></a>}{item.verificationStatus === 'SELF_REPORTED' && <Button variant="ghost" size="sm" onClick={() => void onReview()} className="h-7 gap-1 px-2 text-xs"><Trophy className="h-3 w-3" /> Pedir revisão</Button>}</div></div>;

const OpportunityCard = ({ opportunity, applied, onApply }: { opportunity: Opportunity; applied: boolean; onApply: () => void }) => <div className="rounded-2xl border p-4"><div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">{opportunity.title}</h3><p className="text-xs text-muted-foreground">{opportunity.organizationName}{opportunity.location ? ` · ${opportunity.location}` : ''}</p></div>{opportunity.remote && <Badge variant="secondary">Remoto</Badge>}</div><p className="mt-3 line-clamp-3 text-sm text-muted-foreground">{opportunity.description}</p><div className="mt-3 flex flex-wrap gap-1">{opportunity.requiredSkills.split(',').slice(0, 4).map((skill) => <Badge key={skill} variant="outline" className="text-[10px]">{skill.trim()}</Badge>)}{opportunity.accessibleSupport && <Badge variant="secondary" className="text-[10px]">Acessível</Badge>}</div><Button disabled={applied} onClick={onApply} size="sm" className="mt-4 w-full gap-2">{applied ? <><CheckCircle2 className="h-4 w-4" /> Candidatura enviada</> : <><Send className="h-4 w-4" /> Quero participar</>}</Button></div>;

export default OpportunityOsPage;
