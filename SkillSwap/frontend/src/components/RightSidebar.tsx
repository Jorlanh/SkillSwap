import { useEffect, useState } from 'react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { HomeService } from '@/services/api';
import { Link } from 'react-router-dom';

type ApiCommunity = {
  communityId?: string;
  id?: string;
  name?: string;
  description?: string;
  members?: unknown[] | number;
  memberCount?: number;
  imageUrl?: string;
};

const communityId = (community: ApiCommunity) => String(community.communityId ?? community.id ?? '');

const RightSidebar = () => {
  const [communities, setCommunities] = useState<ApiCommunity[]>([]);
  const [topics, setTopics] = useState<string[]>([]);

  useEffect(() => {
    let active = true;
    Promise.allSettled([HomeService.getCommunities(50), HomeService.getTrendingTopics()]).then(([communitiesResult, topicsResult]) => {
      if (!active) return;
      if (communitiesResult.status === 'fulfilled' && Array.isArray(communitiesResult.value.data)) {
        let searches: string[] = [];
        try {
          const stored = JSON.parse(window.localStorage.getItem('skillswap.searches') || '[]');
          searches = Array.isArray(stored) ? stored.map((value) => String(value).toLowerCase()) : [];
        } catch { searches = []; }
        const ranked = [...communitiesResult.value.data].sort((left, right) => {
          const score = (community: ApiCommunity) => searches.reduce((total, term) => total + (String(community.name || '').toLowerCase().includes(term) || String(community.description || '').toLowerCase().includes(term) ? 1 : 0), 0);
          return score(right) - score(left);
        });
        setCommunities(ranked.slice(0, 3));
      }
      if (topicsResult.status === 'fulfilled' && Array.isArray(topicsResult.value.data)) {
        setTopics(topicsResult.value.data);
      }
    });
    return () => { active = false; };
  }, []);

  return (
    <aside className="hidden xl:block w-64 shrink-0 space-y-6">
      {/* Communities */}
      <div className="bg-card rounded-xl border border-border p-4">
        <h3 className="font-display font-semibold text-foreground mb-3">Comunidades para você</h3>
        {communities.length > 0 ? <div className="space-y-3">
          {communities.map((c) => (
            <div key={communityId(c)} className="flex items-center gap-2 min-w-0">
              <Avatar className="h-8 w-8">
                {c.imageUrl && <img src={c.imageUrl} alt="" className="h-full w-full rounded-full object-cover" />}
                <AvatarFallback>{c.name?.slice(0, 1).toUpperCase() || 'C'}</AvatarFallback>
              </Avatar>
              <Link to={`/comunidades/${communityId(c)}`} className="text-sm text-foreground flex-1 truncate hover:text-skillswap-teal" title={c.name}>{c.name}</Link>
              <span className="text-[10px] text-muted-foreground shrink-0">Recomendação</span>
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
          <Link to="/institucional/sobre" className="block text-sm text-muted-foreground hover:text-foreground transition-colors">Sobre o SkillSwap</Link>
          <a href="mailto:suporte.skillswap@gmail.com" className="block text-sm text-muted-foreground hover:text-foreground transition-colors">Anuncie aqui</a>
          <Link to="/institucional/ajuda" className="block text-sm text-muted-foreground hover:text-foreground transition-colors">Ajuda</Link>
          <Link to="/institucional/noticias" className="block text-sm text-muted-foreground hover:text-foreground transition-colors">Notícias</Link>
          <Link to="/institucional/blog" className="block text-sm text-muted-foreground hover:text-foreground transition-colors">Blog</Link>
        </div>
      </div>

      <div className="text-xs text-muted-foreground space-y-1">
        <p><Link to="/institucional/regras" className="hover:text-foreground">Regras do SkillSwap</Link> | <Link to="/institucional/privacidade" className="hover:text-foreground">Políticas de Privacidade</Link></p>
        <p><Link to="/institucional/contrato" className="hover:text-foreground">Contrato de Usuário</Link> | <Link to="/institucional/termos" className="hover:text-foreground">Termos de Serviço</Link></p>
        <p>SkillSwap</p>
      </div>
    </aside>
  );
};

export default RightSidebar;
