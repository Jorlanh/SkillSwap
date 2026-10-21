import type { User, Post, Conversation, ChatMessage, Community, Notification, Proposal, SwapCoinPackage, AccessLog } from '@/types';

export const currentUser: User = {
  id: '1',
  username: 'MartinScorseseFa',
  handle: '@MU570098',
  avatar: 'https://i.pravatar.cc/150?img=11',
  bio: 'Em busca de aprender o melhor da vida.',
  location: 'Salvador, Bahia',
  joinDate: '27/04/2015',
  following: 328,
  followers: 495,
  skills: ['Java', 'Jardinagem', 'Rust', 'Gastronomia', 'Eletrônica', 'JavaScript', 'MySQL'],
};

export const mockUsers: User[] = [
  { id: '2', username: 'Vi11_5', handle: '@vi11_5', avatar: 'https://i.pravatar.cc/150?img=32', bio: '', location: '', joinDate: '10/01/2020', following: 120, followers: 890, skills: ['Jardinagem'], isVerified: true },
  { id: '3', username: 'coelhorosa', handle: '@cuuutesx', avatar: 'https://i.pravatar.cc/150?img=45', bio: '', location: '', joinDate: '04/02/2022', following: 45, followers: 230, skills: ['Cerâmica', 'Turco'] },
  { id: '4', username: 'Douglas Oliveira', handle: '@dougOliver', avatar: 'https://i.pravatar.cc/150?img=12', bio: '', location: '', joinDate: '15/06/2019', following: 200, followers: 1500, skills: [] },
  { id: '5', username: 'Jorlan Vargas', handle: '@vargas_12', avatar: 'https://i.pravatar.cc/150?img=53', bio: '', location: '', joinDate: '20/03/2021', following: 80, followers: 400, skills: [] },
  { id: '6', username: 'Leon Silva', handle: '@leonSilva', avatar: 'https://i.pravatar.cc/150?img=59', bio: '', location: '', joinDate: '01/09/2018', following: 150, followers: 700, skills: ['Guitarra'] },
  { id: '7', username: 'JacquinJrJr', handle: '@jacquinjr', avatar: 'https://i.pravatar.cc/150?img=14', bio: '', location: '', joinDate: '28/03/2017', following: 300, followers: 2000, skills: ['Culinária'], helpedCount: 574, isVerified: true },
  { id: '8', username: 'iLOVEcook_90', handle: '@ilovecook', avatar: 'https://i.pravatar.cc/150?img=26', bio: '', location: '', joinDate: '04/08/2020', following: 60, followers: 800, skills: ['Culinária'], helpedCount: 301 },
  { id: '9', username: '8283_samurainacozinha', handle: '@samurai8283', avatar: 'https://i.pravatar.cc/150?img=36', bio: '', location: '', joinDate: '07/08/2021', following: 30, followers: 350, skills: ['Culinária'], helpedCount: 159 },
  { id: '10', username: 'shikasapel', handle: '@shikasapel', avatar: 'https://i.pravatar.cc/150?img=22', bio: '', location: '', joinDate: '12/05/2020', following: 75, followers: 500, skills: ['Bordado'], isVerified: true },
  { id: '11', username: 'gatobobo441', handle: '@gatobobo441', avatar: 'https://i.pravatar.cc/150?img=41', bio: '', location: '', joinDate: '03/11/2021', following: 40, followers: 250, skills: [] },
  { id: '12', username: 'tarantula_1', handle: '@tarantula1', avatar: 'https://i.pravatar.cc/150?img=17', bio: '', location: '', joinDate: '08/07/2019', following: 90, followers: 600, skills: [] },
  { id: '13', username: 'Júlia', handle: '@julia', avatar: 'https://i.pravatar.cc/150?img=49', bio: '', location: '', joinDate: '22/04/2020', following: 110, followers: 750, skills: [] },
  { id: '14', username: 'Cristiano', handle: '@cristiano', avatar: 'https://i.pravatar.cc/150?img=60', bio: '', location: '', joinDate: '15/12/2018', following: 200, followers: 1200, skills: [] },
  { id: '15', username: 'HarpyWitch', handle: '@harpywitch', avatar: 'https://i.pravatar.cc/150?img=25', bio: '', location: '', joinDate: '30/01/2021', following: 55, followers: 320, skills: [] },
  { id: '16', username: 'salsichinha', handle: '@salsichinha', avatar: 'https://i.pravatar.cc/150?img=33', bio: '', location: '', joinDate: '18/09/2020', following: 35, followers: 180, skills: [] },
  { id: '17', username: 'CATS4LIFE', handle: '@cats4life', avatar: 'https://i.pravatar.cc/150?img=38', bio: '', location: '', joinDate: '25/06/2021', following: 100, followers: 900, skills: [] },
  { id: '18', username: 'MORGAN', handle: '@morgan', avatar: 'https://i.pravatar.cc/150?img=51', bio: '', location: '', joinDate: '11/03/2019', following: 170, followers: 1100, skills: [] },
  { id: '19', username: 'lerolero', handle: '@lerolero', avatar: 'https://i.pravatar.cc/150?img=56', bio: '', location: '', joinDate: '05/08/2022', following: 20, followers: 140, skills: [] },
  { id: '20', username: 'bellinha_braga', handle: '@bellinha', avatar: 'https://i.pravatar.cc/150?img=47', bio: '', location: '', joinDate: '14/02/2020', following: 130, followers: 650, skills: [] },
];

export const mockPosts: Post[] = [
  {
    id: '1',
    author: mockUsers[2],
    community: 'c/Mildlyinteresting',
    content: 'My dog kinda looks like an oil painting in this photo',
    image: 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=600&h=400&fit=crop',
    comments: 42000,
    shares: 9000,
    upvotes: 10000,
    reposts: 10000,
    createdAt: '2025-05-21',
  },
  {
    id: '2',
    author: mockUsers[3],
    community: 'c/Cats',
    content: 'What is his name? wrong answers only',
    image: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600&h=400&fit=crop',
    comments: 42000,
    shares: 1000,
    upvotes: 10000,
    reposts: 10000,
    createdAt: '2025-05-20',
  },
  {
    id: '3',
    author: mockUsers[4],
    community: 'c/Programação Web Brasil',
    content: 'Busco colegas para troca de habilidades. Tenho 22 anos, sou guitarrista e gostaria de aprender o básico de programação. Quem estiver interessado, por favor, entrar em contato via DM.',
    image: 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&h=400&fit=crop',
    comments: 42000,
    shares: 9000,
    upvotes: 10000,
    reposts: 10000,
    createdAt: '2025-05-19',
  },
];

export const mockConversations: Conversation[] = [
  { id: '1', user: mockUsers[0], lastMessage: 'sobre isso, queria saber...', unreadCount: 0, isRead: true, timestamp: '07 de Abril' },
  { id: '2', user: mockUsers[10], lastMessage: '2 Mensagens não lidas...', unreadCount: 2, isRead: false, timestamp: '06 de Abril' },
  { id: '3', user: mockUsers[11], lastMessage: 'Isso, você precisa de um...', unreadCount: 0, isRead: true, timestamp: '05 de Abril' },
  { id: '4', user: mockUsers[12], lastMessage: '3 Mensagens não lidas...', unreadCount: 3, isRead: false, timestamp: '04 de Abril' },
  { id: '5', user: mockUsers[13], lastMessage: 'q bom q conseguiu :)', unreadCount: 0, isRead: true, timestamp: '03 de Abril' },
  { id: '6', user: mockUsers[14], lastMessage: '4 Mensagens não lidas...', unreadCount: 4, isRead: false, timestamp: '02 de Abril' },
];

export const mockChatMessages: ChatMessage[] = [
  { id: '1', senderId: '1', content: 'ooi!! você é o Vi que vai me ajudar com o jardim?', timestamp: '14:30', isOwn: true },
  { id: '2', senderId: '2', content: 'isso, sou eu!', timestamp: '14:32', isOwn: false },
  { id: '3', senderId: '2', content: 'sobre isso, queria saber sobre quando voce estará disponível para eu te ajudar!! quero começar logo estou ansioso 😄', timestamp: '14:35', isOwn: false },
];

export const mockCommunities: Community[] = [
  { id: '1', name: 'Culinária', members: 73, avatars: ['https://i.pravatar.cc/40?img=1', 'https://i.pravatar.cc/40?img=2'] },
  { id: '2', name: 'Violão', members: 329, avatars: ['https://i.pravatar.cc/40?img=3', 'https://i.pravatar.cc/40?img=4'] },
  { id: '3', name: 'Latim', members: 52, avatars: ['https://i.pravatar.cc/40?img=5', 'https://i.pravatar.cc/40?img=6'] },
  { id: '4', name: 'Mixagem', members: 15, avatars: ['https://i.pravatar.cc/40?img=7', 'https://i.pravatar.cc/40?img=8'] },
  { id: '5', name: 'Decoração de unhas', members: 154, avatars: ['https://i.pravatar.cc/40?img=9', 'https://i.pravatar.cc/40?img=10'] },
  { id: '6', name: 'Jardinagem', members: 85, avatars: ['https://i.pravatar.cc/40?img=11', 'https://i.pravatar.cc/40?img=12'] },
  { id: '7', name: 'Maquiagem', members: 452, avatars: ['https://i.pravatar.cc/40?img=13', 'https://i.pravatar.cc/40?img=14'] },
  { id: '8', name: 'Turco', members: 78, avatars: ['https://i.pravatar.cc/40?img=15', 'https://i.pravatar.cc/40?img=16'] },
];

export const mockNotifications: Notification[] = [
  { id: '1', user: mockUsers[0], action: 'Curtiu o vídeo de', target: 'GabiPires701', image: 'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=80&h=80&fit=crop', type: 'like' },
  { id: '2', user: mockUsers[8], action: 'Demonstrou interesse em', target: 'Bordado', image: 'https://images.unsplash.com/photo-1452587925148-ce544e77e70d?w=80&h=80&fit=crop', type: 'interest' },
  { id: '3', user: mockUsers[9], action: 'Curtiu as fotos de', target: 'LEAOjogos', image: 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=80&h=80&fit=crop', type: 'like' },
  { id: '4', user: mockUsers[9], action: 'Comentou nas fotos de', target: 'LEAOjogos', image: 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=80&h=80&fit=crop', type: 'comment' },
  { id: '5', user: mockUsers[0], action: 'Curtiu o vídeo de', target: 'Penguingordin5', image: 'https://images.unsplash.com/photo-1462888210965-cdf193600fcd?w=80&h=80&fit=crop', type: 'like' },
  { id: '6', user: mockUsers[18], action: 'e quokka', target: 'Estão aprendendo juntos', image: undefined, type: 'learning' },
];

export const mockProposals: Proposal[] = [
  {
    id: '1',
    sender: mockUsers[1],
    skillWanted: 'Turco',
    skillOffered: 'Cerâmica',
    message: 'oii! :)\n\nvi seu perfil, sou muito interessada em turco desde criança e nuca tive oportunidade de, de fato, aprender, acho que seria bom agora já que posso te ajudar com a cerâmica!! <3',
    date: '4 de Março',
    preview: 'Olá,',
  },
  {
    id: '2',
    sender: mockUsers[15],
    skillWanted: 'Java',
    skillOffered: 'Design',
    message: 'hii, so i saw that you want to learn design and I can help!',
    date: '2 de Março',
    preview: 'hii, so i saw that you want to...',
  },
  {
    id: '3',
    sender: mockUsers[16],
    skillWanted: 'Guitarra',
    skillOffered: 'Programação',
    message: 'oi, queria saber se voce tambem quer aprender programação',
    date: '1 de Março',
    preview: 'oi, queria saber se voce tambem..',
  },
  {
    id: '4',
    sender: mockUsers[17],
    skillWanted: 'Rust',
    skillOffered: 'Música',
    message: 'about the lessons abt that thing we talked about',
    date: '28 de Fevereiro',
    preview: 'about the lessons abt that thing...',
  },
];

export const mockSwapCoinPackages: SwapCoinPackage[] = [
  { id: '1', quantity: '100', price: 'R$ 9,90' },
  { id: '2', quantity: '250', price: 'R$ 19,90', bonus: '+7% Bônus' },
  { id: '3', quantity: '500', price: 'R$ 34,90', bonus: '+13% Bônus' },
  { id: '4', quantity: '1000', price: 'R$ 59,90', bonus: '+20% Bônus' },
];

export const mockAccessLogs: AccessLog[] = [
  { id: '1', location: 'Salvador, Bahia', time: '13:44', date: '21/05/2025' },
  { id: '2', location: 'Salvador, Bahia', time: '09:12', date: '20/05/2025' },
  { id: '3', location: 'São Paulo, SP', time: '22:30', date: '18/05/2025' },
  { id: '4', location: 'Salvador, Bahia', time: '14:55', date: '17/05/2025' },
];

export const trendingTopics = [
  'Culturas virais da internet', 'Jogos', 'Informática', 'Filmes',
  'Séries de TV', 'Arte digital', 'Comidas e bebidas', 'Música',
  'Ciências Humanas', 'Esportes',
];

export const suggestedCommunities = [
  { name: 'c/AprenderSpringBoot', avatar: 'https://i.pravatar.cc/40?img=20' },
  { name: 'c/TocarGuitarraBr', avatar: 'https://i.pravatar.cc/40?img=21' },
  { name: 'c/DesenhoDigital', avatar: 'https://i.pravatar.cc/40?img=22' },
];
