export interface User {
  id: string;
  username: string;
  handle: string;
  avatar: string;
  bio?: string;
  location?: string;
  joinDate: string;
  following: number;
  followers: number;
  skills: string[];
  helpedCount?: number;
  isVerified?: boolean;
}

export interface Post {
  id: string;
  author: User;
  community?: string;
  content: string;
  image?: string;
  comments: number;
  shares: number;
  upvotes: number;
  reposts: number;
  createdAt: string;
}

export interface Conversation {
  id: string;
  user: User;
  lastMessage: string;
  unreadCount: number;
  isRead: boolean;
  timestamp: string;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  content: string;
  timestamp: string;
  isOwn: boolean;
}

export interface Community {
  id: string;
  name: string;
  members: number;
  avatars: string[];
}

export interface Notification {
  id: string;
  user: User;
  action: string;
  target: string;
  image?: string;
  type: 'like' | 'interest' | 'comment' | 'learning' | 'follow';
}

export interface Proposal {
  id: string;
  sender: User;
  skillWanted: string;
  skillOffered: string;
  message: string;
  date: string;
  preview: string;
}

export interface SwapCoinPackage {
  id: string;
  quantity: string;
  price: string;
  bonus?: string;
}

export interface AccessLog {
  id: string;
  location: string;
  time: string;
  date: string;
}
