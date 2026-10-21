import { useEffect, useState } from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { MessageSquare, Repeat2, ArrowUp, Share, Send, Heart, Trash2, Reply } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useOutletContext, useNavigate } from 'react-router-dom';
import { PostService, UserService } from '@/services/api';
import { toast } from 'sonner';

interface PostCardProps {
  post: any;
}

const formatNumber = (n: number | undefined | null) => {
  if (n === undefined || n === null) return "0";
  if (n >= 1000) return `${Math.floor(n / 1000)} mil`;
  return n.toString();
};

const PostCard = ({ post }: PostCardProps) => {
  const navigate = useNavigate();
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  
  // A identidade vem da API autenticada. Dados de usuário não são persistidos
  // no navegador para evitar que a interface trate conteúdo adulterável como
  // fonte de autorização.
  const [loggedUser, setLoggedUser] = useState<any>({});

  useEffect(() => {
    UserService.getMe()
      .then(({ data }) => setLoggedUser(data || {}))
      .catch(() => setLoggedUser({}));
  }, []);

  // ESTADOS DO POST
  const [isLiked, setIsLiked] = useState(post.isLikedByMe || false);
  const [likesCount, setLikesCount] = useState(post.likesCount || 0);
  const [isReposted, setIsReposted] = useState(false);
  const [repostsCount, setRepostsCount] = useState(post.repostsCount || 0);
  const [isJoined, setIsJoined] = useState(false);

  // ESTADOS DOS COMENTÁRIOS
  const [showComments, setShowComments] = useState(false);
  const [newComment, setNewComment] = useState("");
  const [commentsList, setCommentsList] = useState<any[]>(post.comments || []);
  const [commentsCount, setCommentsCount] = useState(post.commentsCount || 0);

  const author = post.user || post.author || { username: 'Usuário', avatarUrl: '' };

  // --- AÇÕES DO POST ---

  const handleLike = async () => {
    requireAuth(async () => {
      const originalLiked = isLiked;
      const originalCount = likesCount;
      
      // Optimistic UI - Tipagem (prev: number) adicionada
      setIsLiked(!isLiked);
      setLikesCount((prev: number) => isLiked ? prev - 1 : prev + 1);

      try {
        await PostService.like(post.id);
        if (!originalLiked) toast.success("Post curtido!");
      } catch (error) {
        setIsLiked(originalLiked);
        setLikesCount(originalCount);
        toast.error("Erro ao processar curtida.");
      }
    });
  };

  const handleRepost = () => {
    requireAuth(() => {
      const newStatus = !isReposted;
      setIsReposted(newStatus);
      // Tipagem (prev: number) adicionada
      setRepostsCount((prev: number) => newStatus ? prev + 1 : prev - 1);
      toast.success(newStatus ? "Repostado com sucesso!" : "Repost removido");
    });
  };

  const handleDeletePost = async () => {
    if (!window.confirm("Deseja apagar esta publicação permanentemente?")) return;
    try {
      await PostService.delete(post.id);
      toast.success("Post removido.");
      // O ideal aqui é que o componente pai recarregue a lista (loadPosts)
    } catch (error) {
      toast.error("Erro ao deletar post.");
    }
  };

  const handleShare = () => {
    const url = `${window.location.origin}/perfil/${author.username}`;
    navigator.clipboard.writeText(url);
    toast.success("Link do perfil copiado!");
  };

  const submitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    requireAuth(async () => {
      try {
        const { data: savedComment } = await PostService.comment(post.id, newComment);
        setCommentsList([savedComment, ...commentsList]);
        // Tipagem (prev: number) adicionada
        setCommentsCount((prev: number) => prev + 1);
        setNewComment("");
        toast.success("Comentário enviado!");
      } catch (error) {
        toast.error("Não foi possível enviar o comentário. Tente novamente.");
      }
    });
  };

  const handleLikeComment = (commentId: number) => {
    requireAuth(() => {
      toast.info("Comentário curtido");
      // Aqui chamaria o RatingService ou PostService.likeComment(commentId)
    });
  };

  return (
    <div className="bg-card rounded-lg border border-border p-4 transition-all hover:shadow-sm">
      {/* CABEÇALHO */}
      <div className="flex items-start justify-between mb-3 gap-2">
        <div 
          className="flex items-center gap-3 min-w-0 cursor-pointer group"
          onClick={() => navigate(`/perfil/${author.username}`)}
        >
          <Avatar className="h-10 w-10 shrink-0 border border-border group-hover:border-skillswap-teal transition-colors">
            <AvatarImage src={author.avatarUrl || author.imageUrl || author.avatar} />
            <AvatarFallback>{author.username?.[0]?.toUpperCase()}</AvatarFallback>
          </Avatar>
          
          <div className="min-w-0">
            <p className="text-sm font-bold text-foreground truncate group-hover:text-skillswap-teal transition-colors">
              {author.name || author.username}
            </p>
            <p className="text-xs text-muted-foreground truncate">@{author.username?.toLowerCase()}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
            {!isJoined && author.username !== loggedUser.username && (
            <Button variant="outline" size="sm" className="bg-skillswap-teal text-white border-none h-7 text-[10px] uppercase font-bold" onClick={() => setIsJoined(true)}>
                Juntar-se
            </Button>
            )}
            
            {author.username === loggedUser.username && (
                <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-red-500" onClick={handleDeletePost}>
                    <Trash2 size={16} />
                </Button>
            )}
        </div>
      </div>

      {/* CONTEÚDO */}
      <div className="mb-3 space-y-2">
        {post.title && <h3 className="font-bold text-base">{post.title}</h3>}
        <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">{post.content}</p>
      </div>

      {(post.imageUrl || post.image) && (
        <div className="rounded-xl overflow-hidden mb-3 border border-border bg-muted/20">
          <img src={post.imageUrl || post.image} alt="Post content" className="w-full max-h-[500px] object-cover" />
        </div>
      )}

      {/* AÇÕES (FOOTER) */}
      <div className="flex items-center gap-6 pt-2 border-t border-border">
        <button onClick={() => setShowComments(!showComments)} className={`flex items-center gap-1.5 text-xs font-medium transition-colors ${showComments ? 'text-skillswap-teal' : 'text-muted-foreground hover:text-foreground'}`}>
          <MessageSquare className="h-4 w-4" />
          <span>{formatNumber(commentsCount)}</span>
        </button>

        <button onClick={handleRepost} className={`flex items-center gap-1.5 text-xs font-medium transition-colors ${isReposted ? 'text-green-500' : 'text-muted-foreground hover:text-green-500'}`}>
          <Repeat2 className="h-4 w-4" />
          <span>{formatNumber(repostsCount)}</span>
        </button>

        <button onClick={handleLike} className={`flex items-center gap-1.5 text-xs font-medium transition-colors ${isLiked ? 'text-red-500 font-bold' : 'text-muted-foreground hover:text-red-500'}`}>
          <ArrowUp className={`h-4 w-4 ${isLiked ? 'fill-red-500' : ''}`} />
          <span>{formatNumber(likesCount)}</span>
        </button>

        <button onClick={handleShare} className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground ml-auto">
          <Share className="h-4 w-4" />
        </button>
      </div>

      {/* SEÇÃO DE COMENTÁRIOS EXPANSÍVEL */}
      {showComments && (
        <div className="mt-4 pt-4 border-t border-border animate-in fade-in slide-in-from-top-2">
          <form onSubmit={submitComment} className="flex gap-2 mb-4">
            <Avatar className="h-8 w-8 shrink-0">
              <AvatarImage src={loggedUser.avatarUrl || loggedUser.imageUrl} />
              <AvatarFallback>U</AvatarFallback>
            </Avatar>
            <Input 
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Escreva um comentário..." 
              className="flex-1 h-9 text-xs bg-muted/30 border-none focus-visible:ring-skillswap-teal"
            />
            <Button type="submit" size="sm" disabled={!newComment.trim()} className="bg-skillswap-teal hover:bg-teal-700 h-9">
              <Send size={14} />
            </Button>
          </form>

          <div className="space-y-4">
            {commentsList.map((comment: any) => (
              <div key={comment.id} className="flex gap-3 group">
                <Avatar className="h-7 w-7 shrink-0 cursor-pointer" onClick={() => navigate(`/perfil/${comment.user?.username}`)}>
                  <AvatarImage src={comment.user?.avatarUrl || comment.user?.imageUrl || comment.user?.avatar} />
                  <AvatarFallback className="text-[10px]">{comment.user?.username?.[0]?.toUpperCase()}</AvatarFallback>
                </Avatar>
                <div className="flex-1">
                  <div className="bg-muted/40 p-2 px-3 rounded-2xl rounded-tl-sm relative">
                    <p className="text-[11px] font-bold text-skillswap-teal mb-0.5">@{comment.user?.username}</p>
                    <p className="text-xs text-foreground/90">{comment.content}</p>
                    
                    <button 
                        onClick={() => handleLikeComment(comment.id)}
                        className="absolute -right-2 -bottom-2 bg-card border border-border rounded-full p-1 shadow-sm hover:scale-110 transition-transform text-muted-foreground hover:text-red-500"
                    >
                        <Heart size={10} className="fill-current" />
                    </button>
                  </div>
                  
                  <div className="flex items-center gap-3 mt-1 ml-1">
                    <button className="text-[10px] font-bold text-muted-foreground hover:text-foreground flex items-center gap-1">
                        <Reply size={10} /> Responder
                    </button>
                    {comment.user?.username === loggedUser.username && (
                        <button className="text-[10px] font-bold text-muted-foreground hover:text-red-500">
                            Excluir
                        </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default PostCard;
