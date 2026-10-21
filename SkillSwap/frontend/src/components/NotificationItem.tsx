import type { Notification } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Heart, MessageCircle, Star, Link2, ChevronRight } from 'lucide-react';

interface NotificationItemProps {
  notification: Notification;
}

const typeIcons = {
  like: Heart,
  interest: Star,
  comment: MessageCircle,
  learning: Link2,
  follow: Heart,
};

const NotificationItem = ({ notification }: NotificationItemProps) => {
  const Icon = typeIcons[notification.type];

  return (
    <div className="flex items-center gap-4 py-4 border-b border-border">
      <div className="relative">
        <Avatar className="h-14 w-14">
          <AvatarImage src={notification.user.avatar} />
          <AvatarFallback>{notification.user.username[0]}</AvatarFallback>
        </Avatar>
        <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-destructive flex items-center justify-center">
          <Icon className="h-3 w-3 text-primary-foreground" />
        </div>
      </div>

      <div className="flex-1">
        <p className="text-sm text-foreground">
          <strong>{notification.user.username}</strong>
        </p>
        <p className="text-sm text-muted-foreground">
          {notification.action} <strong>{notification.target}</strong>
        </p>
      </div>

      {notification.image && (
        <img src={notification.image} alt="" className="w-14 h-14 rounded-lg object-cover" />
      )}

      <ChevronRight className="h-5 w-5 text-muted-foreground" />
    </div>
  );
};

export default NotificationItem;
