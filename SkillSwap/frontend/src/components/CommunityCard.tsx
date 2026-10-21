import type { Community } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { useOutletContext } from 'react-router-dom';

interface CommunityCardProps {
  community: Community;
  onJoin?: () => void;
}

const CommunityCard = ({ community, onJoin }: CommunityCardProps) => {
  // ACESSO À BARREIRA GLOBAL
  const context = useOutletContext<{ requireAuth?: (action: () => void) => void }>();

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
      <div className="flex -space-x-2 mb-3">
        {community.avatars.map((a, i) => (
          <Avatar key={i} className="h-10 w-10 border-2 border-card">
            <AvatarImage src={a} />
            <AvatarFallback>U</AvatarFallback>
          </Avatar>
        ))}
      </div>
      <p className="text-sm text-foreground mb-3">
        <strong>{community.members}</strong> Membros
      </p>
      <Button
        variant="outline"
        size="sm"
        className="bg-skillswap-light-blue border-foreground/20 text-foreground hover:bg-accent"
        onClick={handleJoinClick}
      >
        Entrar
      </Button>
    </div>
  );
};

export default CommunityCard;