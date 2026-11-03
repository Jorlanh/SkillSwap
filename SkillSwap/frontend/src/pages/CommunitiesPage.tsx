import { useEffect, useState } from 'react';
import CommunityCard from '@/components/CommunityCard';
import { ChevronLeft } from 'lucide-react';
import { Link, useOutletContext } from 'react-router-dom';
import { HomeService } from '@/services/api';
import type { Community } from '@/types';

type ApiCommunity = {
  communityId?: string;
  id?: string;
  name?: string;
  members?: unknown[] | number;
  memberCount?: number;
};

const toCommunity = (community: ApiCommunity): Community => ({
  id: String(community.communityId ?? community.id ?? community.name ?? ''),
  name: community.name ?? 'Comunidade',
  members: Array.isArray(community.members) ? community.members.length : community.memberCount ?? Number(community.members ?? 0),
  avatars: [],
});

const CommunitiesPage = () => {
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [joined, setJoined] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    HomeService.getCommunities(50)
      .then(({ data }) => setCommunities(Array.isArray(data) ? data.map(toCommunity) : []))
      .catch(() => setError(true))
      .finally(() => setIsLoading(false));
  }, []);

  const join = (id: string) => requireAuth(() => {
    HomeService.joinCommunity(id).then(() => setJoined((current) => [...new Set([...current, id])]));
  });

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-skillswap-card-bg rounded-2xl p-6">
        <div className="flex items-center justify-between mb-2">
          <Link to="/" className="inline-flex items-center gap-1 text-foreground font-semibold hover:opacity-80">
            <ChevronLeft className="h-5 w-5" /> Voltar
          </Link>
        </div>

        <h1 className="font-display font-bold text-3xl text-foreground text-center mb-1">Comunidade</h1>
        <p className="text-center text-muted-foreground mb-8">Venha fazer parte de alguma também! :)</p>

        {isLoading ? <p className="py-10 text-center text-muted-foreground">Carregando comunidades...</p> : error ? <p className="py-10 text-center text-destructive">Não foi possível carregar as comunidades.</p> : communities.length === 0 ? <p className="py-10 text-center text-muted-foreground">Ainda não há comunidades cadastradas.</p> : <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {communities.map((community) => (
            <CommunityCard 
              key={community.id} 
              community={community} 
              joined={joined.includes(community.id)}
              onJoin={() => join(community.id)}
            />
          ))}
        </div>}
      </div>
    </div>
  );
};

export default CommunitiesPage;
