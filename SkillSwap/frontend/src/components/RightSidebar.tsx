import { useEffect, useState } from 'react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { HomeService } from '@/services/api';
import { useOutletContext } from 'react-router-dom';

type ApiCommunity = {
  communityId?: string;
  id?: string;
  name?: string;
  members?: unknown[] | number;
  memberCount?: number;
};

const communityId = (community: ApiCommunity) => String(community.communityId ?? community.id ?? '');

const RightSidebar = () => {
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  const [communities, setCommunities] = useState<ApiCommunity[]>([]);
  const [topics, setTopics] = useState<string[]>([]);
  const [joined, setJoined] = useState<string[]>([]);

  useEffect(() => {
    let active = true;
    Promise.allSettled([HomeService.getCommunities(3), HomeService.getTrendingTopics()]).then(([communitiesResult, topicsResult]) => {
      if (!active) return;
      if (communitiesResult.status === 'fulfilled' && Array.isArray(communitiesResult.value.data)) {
        setCommunities(communitiesResult.value.data);
      }
      if (topicsResult.status === 'fulfilled' && Array.isArray(topicsResult.value.data)) {
        setTopics(topicsResult.value.data);
      }
    });
    return () => { active = false; };
  }, []);

  const join = (id: string) => {
    if (!id) return;
    requireAuth(() => {
      HomeService.joinCommunity(id).then(() => setJoined((current) => [...new Set([...current, id])]));
    });
  };

  return (
    <aside className="hidden xl:block w-64 shrink-0 space-y-6">
      {/* Communities */}
      <div className="bg-card rounded-xl border border-border p-4">
        <h3 className="font-display font-semibold text-foreground mb-3">Comunidades para você</h3>
        {communities.length > 0 ? <div className="space-y-3">
          {communities.map((c) => (
            <div key={communityId(c)} className="flex items-center gap-2 min-w-0">
              <Avatar className="h-8 w-8">
                <AvatarFallback>{c.name?.slice(0, 1).toUpperCase() || 'C'}</AvatarFallback>
              </Avatar>
              <span className="text-sm text-foreground flex-1 truncate" title={c.name}>{c.name}</span>
              <Button size="sm" disabled={joined.includes(communityId(c))} onClick={() => join(communityId(c))} className="bg-skillswap-teal text-primary-foreground text-xs h-6 px-2 hover:opacity-90 shrink-0">
                {joined.includes(communityId(c)) ? 'Membro' : 'Juntar-se'}
              </Button>
            </div>
          ))}
        </div> : <p className="text-sm text-muted-foreground">Nenhuma comunidade disponível no momento.</p>}
      </div>

      {/* Topics */}
      {topics.length > 0 && <div>
        <h3 className="font-display font-semibold text-foreground mb-3">Assuntos</h3>
        <div className="space-y-1">
          {topics.map((t) => (
            <button key={t} className="block text-sm text-muted-foreground hover:text-foreground transition-colors">
              {t}
            </button>
          ))}
        </div>
      </div>}

      {/* Resources */}
      <div>
        <h3 className="font-display font-semibold text-foreground mb-3">Recursos</h3>
        <div className="space-y-1">
          {['Sobre o SkillSwap', 'Anuncie aqui', 'Ajuda', 'Notícias', 'Blog'].map((r) => (
            <button key={r} className="block text-sm text-muted-foreground hover:text-foreground transition-colors">
              {r}
            </button>
          ))}
        </div>
      </div>

      <div className="text-xs text-muted-foreground space-y-1">
        <p>Regras do SkillSwap | Políticas de Privacidade</p>
        <p>Contrato de Usuário | Termos de Serviço</p>
        <p>SkillSwap, Inc. © 2025</p>
      </div>
    </aside>
  );
};

export default RightSidebar;
