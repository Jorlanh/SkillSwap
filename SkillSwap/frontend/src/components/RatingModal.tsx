import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import RatingStars from '@/components/RatingStars';
import VerifiedBadge from '@/components/VerifiedBadge';
import type { User } from '@/types';

interface RatingModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user?: User | null; // Alterado para opcional para evitar crash
  proposalId: string;
  onSubmit?: (data: { proposalId: string; stars: number; comment: string }) => void;
}

const RatingModal = ({ open, onOpenChange, user, proposalId, onSubmit }: RatingModalProps) => {
  const [stars, setStars] = useState(0);
  const [comment, setComment] = useState('');

  // SE NÃO HOUVER USUÁRIO, NÃO RENDERIZA NADA (EVITA O ERRO DE 'avatar')
  if (!user) return null;

  const handleSubmit = () => {
    onSubmit?.({ proposalId, stars, comment });
    setStars(0);
    setComment('');
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md bg-card border-border">
        <DialogHeader>
          <DialogTitle className="font-display text-center text-xl font-bold">Avaliar Troca</DialogTitle>
          <DialogDescription className="text-center text-muted-foreground">Como foi sua experiência de aprendizado?</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center gap-4 py-4">
          <Avatar className="h-20 w-20 border-2 border-skillswap-teal">
            {/* Suporte para 'avatar' ou 'avatarUrl' vindo do Java */}
            <AvatarImage src={user.avatar || (user as any).avatarUrl} />
            <AvatarFallback className="text-xl font-bold">
              {user.username ? user.username[0].toUpperCase() : '?'}
            </AvatarFallback>
          </Avatar>
          
          <div className="flex items-center gap-1.5">
            <h3 className="font-display font-bold text-lg text-foreground">{user.username}</h3>
            {user.isVerified && <VerifiedBadge size={16} />}
          </div>

          <div className="py-2">
            <RatingStars value={stars} onChange={setStars} size={32} />
          </div>

          <Textarea
            placeholder="Conte-nos um pouco sobre o que achou da aula..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            className="min-h-[100px] bg-muted/20 border-border focus-visible:ring-skillswap-teal resize-none"
          />

          <Button
            onClick={handleSubmit}
            disabled={stars === 0}
            className="w-full bg-skillswap-teal hover:bg-teal-700 text-white font-bold h-11"
          >
            Submeter Avaliação
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default RatingModal;