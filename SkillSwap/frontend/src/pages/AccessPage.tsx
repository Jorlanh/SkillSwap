import { useEffect, useState } from 'react';
import { AccessService } from '@/services/api';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { BriefcaseBusiness, CheckCircle2, Sparkles } from 'lucide-react';

type Recommendation = {
  opportunityId: string;
  organizationName: string;
  title: string;
  description: string;
  remote: boolean;
  accessibleSupport: boolean;
  score: number;
  explanation: string;
};

const AccessPage = () => {
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [overview, setOverview] = useState<any>(null);

  useEffect(() => {
    Promise.all([AccessService.getOverview(), AccessService.getRecommendations()])
      .then(([overviewResponse, recommendationsResponse]) => {
        setOverview(overviewResponse.data);
        setRecommendations(recommendationsResponse.data);
      })
      .catch(() => undefined);
  }, []);

  return (
    <div className="max-w-6xl mx-auto p-4 space-y-6">
      <section className="rounded-3xl bg-gradient-to-br from-skillswap-teal/20 via-card to-card border border-border p-8">
        <div className="flex items-start gap-4">
          <div className="rounded-2xl bg-skillswap-teal p-3 text-white"><BriefcaseBusiness /></div>
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-skillswap-teal">SkillSwap Access</p>
            <h1 className="mt-2 text-3xl font-black">Aprendizagem que se transforma em oportunidade</h1>
            <p className="mt-3 max-w-3xl text-muted-foreground">Uma experiência para qualquer pessoa, com foco principal em remover barreiras de acessibilidade, mostrar habilidades na prática e explicar por que cada curso, mentor, projeto ou vaga foi recomendado.</p>
          </div>
        </div>
        {overview && <div className="mt-6 grid gap-3 sm:grid-cols-3 text-sm">
          <div className="rounded-2xl bg-background/70 p-4"><b>Perfil</b><br />{overview.profileStatus}</div>
          <div className="rounded-2xl bg-background/70 p-4"><b>Recomendação</b><br />{overview.recommendationStrategy}</div>
          <div className="rounded-2xl bg-background/70 p-4"><b>Privacidade</b><br />{overview.privacy}</div>
        </div>}
      </section>

      <div className="flex items-center justify-between gap-4">
        <div><h2 className="text-2xl font-black">Oportunidades para você</h2><p className="text-sm text-muted-foreground">Cada resultado apresenta os sinais usados pelo algoritmo, sem caixa-preta.</p></div>
        <Badge variant="outline"><Sparkles className="mr-1 h-3 w-3" /> explicável</Badge>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {recommendations.map((item) => (
          <Card key={item.opportunityId} className="rounded-3xl">
            <CardHeader><CardTitle className="flex items-start justify-between gap-3"><span>{item.title}<small className="mt-1 block text-sm font-normal text-muted-foreground">{item.organizationName}</small></span><Badge>{item.score}%</Badge></CardTitle></CardHeader>
            <CardContent className="space-y-4"><p className="text-sm text-muted-foreground">{item.description}</p><div className="flex flex-wrap gap-2 text-xs">{item.remote && <Badge variant="secondary">remoto</Badge>}{item.accessibleSupport && <Badge variant="secondary"><CheckCircle2 className="mr-1 h-3 w-3" />apoio de acessibilidade</Badge>}</div><div className="rounded-2xl bg-muted/50 p-4 text-sm"><b>Por que apareceu:</b> {item.explanation}</div></CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default AccessPage;
