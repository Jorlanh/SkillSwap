import { useEffect, useState } from 'react';
import CommunityCard from '@/components/CommunityCard';
import { ChevronLeft } from 'lucide-react';
import { Plus, X } from 'lucide-react';
import { Link, useOutletContext } from 'react-router-dom';
import { HomeService } from '@/services/api';
import type { Community } from '@/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';

type ApiCommunity = {
  communityId?: string;
  id?: string;
  name?: string;
  members?: unknown[] | number;
  memberCount?: number;
  description?: string;
};

const toCommunity = (community: ApiCommunity): Community => ({
  id: String(community.communityId ?? community.id ?? community.name ?? ''),
  name: community.name ?? 'Comunidade',
  members: Array.isArray(community.members) ? community.members.length : community.memberCount ?? Number(community.members ?? 0),
  avatars: [],
  description: community.description,
});

const CommunitiesPage = () => {
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [joined, setJoined] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    HomeService.getCommunities(50)
      .then(({ data }) => setCommunities(Array.isArray(data) ? data.map(toCommunity) : []))
      .catch(() => setError(true))
      .finally(() => setIsLoading(false));
  }, []);

  const join = (id: string) => requireAuth(() => {
    HomeService.joinCommunity(id)
      .then(() => setJoined((current) => [...new Set([...current, id])]))
      .catch(() => toast.error('Não foi possível entrar nessa comunidade agora.'));
  });

  const openCreate = () => requireAuth(() => setShowCreate(true));

  const create = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!newName.trim()) {
      toast.error('Digite um nome para a comunidade.');
      return;
    }
    setCreating(true);
    try {
      const { data } = await HomeService.createCommunity({ name: newName.trim(), description: newDescription.trim() || undefined });
      const created = toCommunity(data);
      setCommunities((current) => [created, ...current]);
      setJoined((current) => [...new Set([...current, created.id])]);
      setNewName('');
      setNewDescription('');
      setShowCreate(false);
      toast.success('Comunidade criada. Você já entrou como primeiro membro.');
    } catch {
      toast.error('Não foi possível criar a comunidade agora.');
    } finally {
      setCreating(false);
    }
  };

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

        <div className="mb-8">
          {!showCreate ? (
            <Button type="button" onClick={openCreate} className="w-full sm:w-auto bg-skillswap-teal hover:bg-teal-700 text-white">
              <Plus className="h-4 w-4 mr-2" /> Criar comunidade
            </Button>
          ) : (
            <form onSubmit={create} className="rounded-xl border border-border bg-card p-4 space-y-3" aria-labelledby="create-community-title">
              <div className="flex items-center justify-between gap-3">
                <h2 id="create-community-title" className="font-semibold text-foreground">Nova comunidade</h2>
                <button type="button" onClick={() => setShowCreate(false)} aria-label="Fechar formulário de comunidade" className="p-1 rounded hover:bg-muted"><X className="h-5 w-5" /></button>
              </div>
              <label className="block text-sm font-medium text-foreground" htmlFor="community-name">Nome</label>
              <Input id="community-name" value={newName} onChange={(event) => setNewName(event.target.value)} maxLength={120} required placeholder="Ex.: Aprender desenho" />
              <label className="block text-sm font-medium text-foreground" htmlFor="community-description">Descrição (opcional)</label>
              <textarea id="community-description" value={newDescription} onChange={(event) => setNewDescription(event.target.value)} maxLength={1000} rows={3} placeholder="Sobre o que as pessoas vão conversar?" className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm" />
              <Button type="submit" disabled={creating} className="bg-skillswap-teal hover:bg-teal-700 text-white">{creating ? 'Criando...' : 'Criar comunidade'}</Button>
            </form>
          )}
        </div>

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
