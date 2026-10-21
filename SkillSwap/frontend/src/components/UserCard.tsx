import type { User } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { ChevronRight } from 'lucide-react';
import VerifiedBadge from '@/components/VerifiedBadge';

interface UserCardProps {
  user: User;
  onClick?: () => void;
}

const UserCard = ({ user, onClick }: UserCardProps) => {
  return (
    <div
      className="flex items-center justify-between bg-skillswap-card-bg rounded-xl p-4 cursor-pointer hover:opacity-90 transition-opacity"
      onClick={onClick}
    >
      <div className="flex items-center gap-4">
        <Avatar className="h-16 w-16">
          <AvatarImage src={user.avatar} />
          <AvatarFallback>{user.username[0]}</AvatarFallback>
        </Avatar>
        <div>
          <div className="flex items-center gap-1.5">
            <h3 className="font-display font-bold text-foreground">{user.username}</h3>
            {user.isVerified && <VerifiedBadge size={16} />}
          </div>
          {user.helpedCount !== undefined && (
            <p className="text-sm text-muted-foreground">
              Ajudou <strong className="text-foreground">{user.helpedCount}</strong> pessoas
            </p>
          )}
          <p className="text-sm text-muted-foreground">Desde {user.joinDate}</p>
        </div>
      </div>
      <ChevronRight className="h-8 w-8 text-foreground" />
    </div>
  );
};

export default UserCard;
