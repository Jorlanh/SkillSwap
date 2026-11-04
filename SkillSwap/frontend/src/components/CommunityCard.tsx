import type { Community } from '@/types';
import { useEffect, useState } from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Link, useOutletContext } from 'react-router-dom';
import { onIdTokenChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { CommunityService } from '@/services/api';

interface CommunityCardProps {
  community: Community;
  onJoin?: () => void;
  joined?: boolean;
}

const CommunityCard = ({ community, onJoin, joined = false }: CommunityCardProps) => {
  const [membership, setMembership] = useState(community.membershipStatus || (joined || community.joined ? 'APPROVED' : 'NONE'));
  // ACESSO À BARREIRA GLOBAL
  const context = useOutletContext<{ requireAuth?: (action: () => void) => void }>();

  useEffect(() => {
    const unsubscribe = onIdTokenChanged(auth, (firebaseUser) => {
      if (!firebaseUser || !community.id) {
        setMembership(joined || community.joined ? 'APPROVED' : 'NONE');
        return;
      }
      CommunityService.getMembership(community.id)
        .then(({ data }) => setMembership(String(data || 'NONE')))
        .catch(() => setMembership(joined || community.joined ? 'APPROVED' : 'NONE'));
    });
    return unsubscribe;
  }, [community.id, community.joined, community.membershipStatus, joined]);

  const handleJoinClick = () => {
    // Se o contexto existir (veio via MainLayout), aplica a barreira
    if (context?.requireAuth) {
      context.requireAuth(() => {
        if (onJoin) onJoin();
      });
    } else {
      // Fallback caso seja usado fora do layout com barreira
      if (onJoin) onJoin();
    }
  };

  return (
    <div className="bg-skillswap-light-blue rounded-xl p-5 flex flex-col items-center text-center">
      <h3 className="font-display font-bold text-foreground mb-3">{community.name}</h3>
      {community.imageUrl && <img src={community.imageUrl} alt={`Imagem da comunidade ${community.name}`} className="mb-3 h-16 w-16 rounded-full object-cover" />}
      <div className="flex -space-x-2 mb-3">
        {community.avatars.map((a, i) => (
          <Avatar key={i} className="h-10 w-10 border-2 border-card">
            <AvatarImage src={a} />
            <AvatarFallback>U</AvatarFallback>
          </Avatar>
        ))}
        {community.avatars.length === 0 && (
          <Avatar className="h-10 w-10 border-2 border-card">
            <AvatarFallback>{community.name.slice(0, 1).toUpperCase()}</AvatarFallback>
          </Avatar>
        )}
      </div>
      <p className="text-sm text-foreground mb-3">
        <strong>{community.members}</strong> Membros
      </p>
      <div className="flex flex-wrap items-center justify-center gap-2">
        {membership === 'PENDING' ? (
          <span className="rounded-md bg-amber-100 px-3 py-2 text-sm font-semibold text-amber-800">Aguardando</span>
        ) : membership === 'APPROVED' || membership === 'ADMIN' || membership === 'OWNER' || joined || community.joined ? (
          <Link to={`/comunidades/${community.id}`} className="rounded-md bg-skillswap-teal px-3 py-2 text-sm font-semibold text-white hover:bg-teal-700">Abrir</Link>
        ) : (
          <>
            <Button
              variant="outline"
              size="sm"
              className="bg-skillswap-light-blue border-foreground/20 text-foreground hover:bg-accent"
              onClick={handleJoinClick}
            >
              Entrar
            </Button>
            <Link to={`/comunidades/${community.id}`} className="text-sm font-semibold text-skillswap-teal hover:underline">Abrir</Link>
          </>
        )}
      </div>
    </div>
  );
};

export default CommunityCard;
