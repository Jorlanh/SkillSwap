import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { MessageSquare, Repeat2, ArrowUp, Share, Send, Heart, Trash2, Reply } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useOutletContext, useNavigate } from 'react-router-dom';
import { HomeService, ProfileService, UserService } from '@/services/api';
import { auth } from '@/lib/firebase';
import { onIdTokenChanged } from 'firebase/auth';
import { toast } from 'sonner';
import VerifiedBadge from '@/components/VerifiedBadge';

interface PostCardProps { post: any; }

const formatNumber = (n: number | undefined | null) => {
  if (n === undefined || n === null) return '0';
  if (n >= 1000) return `${Math.floor(n / 1000)} mil`;
  return n.toString();
};

const getCommentId = (comment: any): number | null => {
  const value = comment?.commentId ?? comment?.id;
  const numeric = Number(value);
  return Number.isInteger(numeric) && numeric > 0 ? numeric : null;
};

const getAvatar = (person: any) => person?.avatarUrl || person?.imageUrl || person?.avatar || person?.profile?.imageUrl || '';

const buildCommentTree = (comments: any[]) => {
  const nodes = comments.map((comment) => ({ ...comment, replies: [] as any[] }));
  const byId = new Map<number, any>();
  nodes.forEach((node) => {
    const id = getCommentId(node);
    if (id) byId.set(id, node);
  });
  const roots: any[] = [];
  nodes.forEach((node) => {
    const parentId = getCommentId({ id: node.parentCommentId });
    const parent = parentId ? byId.get(parentId) : undefined;
    if (parent && parent !== node) parent.replies.push(node);
    else roots.push(node);
  });
  return roots;
};

const PostCard = ({ post }: PostCardProps) => {
  const navigate = useNavigate();
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  const [loggedUser, setLoggedUser] = useState<any>({});
  const [isLiked, setIsLiked] = useState(Boolean(post.isLikedByMe));
  const [likesCount, setLikesCount] = useState(post.likesCount || 0);
  const [isReposted, setIsReposted] = useState(Boolean(post.isRepostedByMe));
  const [repostsCount, setRepostsCount] = useState(post.repostsCount || 0);
  const [showComments, setShowComments] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [commentsList, setCommentsList] = useState<any[]>(post.comments || []);
  const [commentsCount, setCommentsCount] = useState(post.commentsCount || 0);
  const [replyTo, setReplyTo] = useState<number | null>(null);
  const [commentLikes, setCommentLikes] = useState<Record<number, number>>({});

  const author = post.user || post.author || { username: 'Usuário', avatarUrl: '' };
  const postId = post.postId ?? post.id;
  const communityId = post.communityId ?? post.community?.communityId ?? post.community?.id;
  const commentTree = useMemo(() => buildCommentTree(commentsList), [commentsList]);

  useEffect(() => {
    let active = true;
    const unsubscribe = onIdTokenChanged(auth, async (firebaseUser) => {
      if (!firebaseUser) { if (active) setLoggedUser({}); return; }
      try {
        const { data } = await UserService.getMe();
        if (active) setLoggedUser(data || {});
      } catch { if (active) setLoggedUser({}); }
    });
    return () => { active = false; unsubscribe(); };
  }, []);

  const refreshComments = useCallback(async () => {
    if (!postId) return;
    const { data } = await HomeService.getPostComments(postId);
    const list = Array.isArray(data) ? data : [];
    setCommentsList(list);
    setCommentLikes(Object.fromEntries(list.map((comment: any) => [getCommentId(comment), Number(comment.likesCount ?? 0)])));
  }, [postId]);

  useEffect(() => {
    if (!showComments) return;
    refreshComments().catch(() => toast.error('Não foi possível carregar os comentários agora.'));
  }, [showComments, refreshComments]);

  const handleLike = async () => {
    requireAuth(async () => {
      const originalLiked = isLiked;
      const originalCount = likesCount;
      setIsLiked(!isLiked);
      setLikesCount((previous: number) => isLiked ? Math.max(0, previous - 1) : previous + 1);
      try {
        await HomeService.likePost(postId);
        if (!originalLiked) toast.success('Post curtido!');
      } catch {
        setIsLiked(originalLiked);
        setLikesCount(originalCount);
        toast.error('Erro ao processar curtida.');
      }
    });
  };

  const handleRepost = () => requireAuth(async () => {
    try {
      await HomeService.repost(postId);
      const next = !isReposted;
      setIsReposted(next);
      setRepostsCount((previous: number) => Math.max(0, next ? previous + 1 : previous - 1));
      toast.success(next ? 'Repostado com sucesso!' : 'Repost removido.');
    } catch { toast.error('Não foi possível repostar agora.'); }
  });

  const handleDeletePost = async () => {
    if (!window.confirm('Deseja apagar esta publicação permanentemente?')) return;
    try { await ProfileService.deleteMyPost(postId); toast.success('Post removido.'); }
    catch { toast.error('Erro ao deletar post.'); }
  };

  const handleShare = () => requireAuth(async () => {
    try {
      const { data } = await HomeService.generateShareLink(postId);
      const url = typeof data === 'string' ? data : data?.shareUrl;
      if (!url) throw new Error('link ausente');
      await navigator.clipboard.writeText(url);
      toast.success('Link da publicação copiado!');
    } catch { toast.error('Não foi possível compartilhar agora.'); }
  });

  const submitComment = async (event: FormEvent) => {
    event.preventDefault();
    if (!newComment.trim()) return;
    requireAuth(async () => {
      try {
        // Apenas IDs reais do banco podem ser enviados. IDs "local-*" nunca chegam ao backend.
        await HomeService.comment(postId, newComment.trim(), replyTo);
        await refreshComments();
        setCommentsCount((previous: number) => previous + 1);
        setNewComment('');
        setReplyTo(null);
        toast.success(replyTo ? 'Resposta enviada!' : 'Comentário enviado!');
      } catch { toast.error('Não foi possível enviar o comentário. Tente novamente.'); }
    });
  };

  const handleLikeComment = (commentId: number | null) => {
    if (!commentId) return;
    requireAuth(async () => {
      try {
        const { data } = await HomeService.likeComment(commentId);
        setCommentLikes((current) => ({ ...current, [commentId]: Number(data ?? current[commentId] ?? 0) }));
      } catch { toast.error('Não foi possível curtir o comentário.'); }
    });
  };

  const handleDeleteComment = (commentId: number | null) => {
    if (!commentId) return;
    requireAuth(async () => {
      try {
        await HomeService.deleteComment(commentId);
        await refreshComments();
        setCommentsCount((previous: number) => Math.max(0, previous - 1));
        toast.success('Comentário excluído.');
      } catch { toast.error('Não foi possível excluir o comentário.'); }
    });
  };

  const renderComment = (comment: any, depth = 0): JSX.Element => {
    const commentId = getCommentId(comment);
    const username = comment.user?.username || comment.username || 'usuário';
    const commentUser = comment.user || { username, avatarUrl: comment.avatarUrl, verifiedBadge: comment.verifiedBadge };
    return (
      <div key={`${commentId ?? 'comment'}-${comment.createdAt ?? comment.content}`} className={depth > 0 ? 'ml-7 border-l-2 border-border pl-3' : ''}>
        <div className="flex gap-3 group">
          <Avatar className="h-7 w-7 shrink-0 cursor-pointer" onClick={() => navigate(`/perfil/${username}`)}>
            <AvatarImage src={comment.avatarUrl || getAvatar(comment.user)} alt={`Foto de ${username}`} />
            <AvatarFallback className="text-[10px]">{username[0]?.toUpperCase()}</AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0">
            <div className="bg-muted/40 p-2 px-3 rounded-2xl rounded-tl-sm relative">
              <p className="text-[11px] font-bold text-skillswap-teal mb-0.5 flex items-center gap-1">@{username}<VerifiedBadge user={commentUser} size={13} /></p>
              <p className="text-xs text-foreground/90 whitespace-pre-wrap">{comment.content}</p>
              <button type="button" onClick={() => handleLikeComment(commentId)} disabled={!commentId} aria-label="Curtir comentário" className="absolute -right-2 -bottom-2 bg-card border border-border rounded-full px-1.5 py-1 shadow-sm hover:scale-110 transition-transform text-muted-foreground hover:text-red-500 disabled:opacity-50"><Heart size={10} className="inline fill-current" /> <span className="text-[10px] ml-0.5">{commentId ? (commentLikes[commentId] ?? comment.likesCount ?? 0) : 0}</span></button>
            </div>
            <div className="flex items-center gap-3 mt-1 ml-1">
              <button type="button" disabled={!commentId} onClick={() => setReplyTo(commentId)} className="text-[10px] font-bold text-muted-foreground hover:text-foreground flex items-center gap-1 disabled:opacity-50"><Reply size={10} /> Responder</button>
              {username === loggedUser.username && commentId && <button type="button" onClick={() => handleDeleteComment(commentId)} className="text-[10px] font-bold text-muted-foreground hover:text-red-500">Excluir</button>}
            </div>
          </div>
        </div>
        {comment.replies?.length > 0 && <div className="mt-3 space-y-3">{comment.replies.map((reply: any) => renderComment(reply, depth + 1))}</div>}
      </div>
    );
  };

  const ownPost = author.userId && loggedUser.userId ? author.userId === loggedUser.userId : author.username === loggedUser.username;
  return (
    <div className="bg-card rounded-lg border border-border p-4 transition-all hover:shadow-sm">
      <div className="flex items-start justify-between mb-3 gap-2">
        <button type="button" className="flex items-center gap-3 min-w-0 cursor-pointer group text-left" onClick={() => navigate(`/perfil/${author.username}`)}>
          <Avatar className="h-10 w-10 shrink-0 border border-border group-hover:border-skillswap-teal transition-colors"><AvatarImage src={getAvatar(author)} alt={`Foto de ${author.name || author.username}`} /><AvatarFallback>{author.username?.[0]?.toUpperCase()}</AvatarFallback></Avatar>
          <span className="min-w-0"><span className="text-sm font-bold text-foreground truncate group-hover:text-skillswap-teal transition-colors flex items-center gap-1">{author.name || author.username}<VerifiedBadge user={author} size={18} /></span><span className="text-xs text-muted-foreground truncate block">@{author.username?.toLowerCase()}</span></span>
        </button>
        {ownPost && <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-red-500" onClick={handleDeletePost} aria-label="Excluir publicação"><Trash2 size={16} /></Button>}
      </div>

      <div className="mb-3 space-y-2">
        {post.title && <h3 className="font-bold text-base">{post.title}</h3>}
        <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">{post.content}</p>
        {(post.community || post.communityName) && <button type="button" onClick={() => communityId && navigate(`/comunidades/${communityId}`)} className="text-xs text-skillswap-teal hover:underline text-left">Publicado em {post.communityName || post.community}</button>}
        {post.createdAt && <time dateTime={post.createdAt} className="text-[11px] text-muted-foreground block">Publicado em {new Date(post.createdAt).toLocaleString('pt-BR')}</time>}
      </div>
      {(post.imageUrl || post.image) && <div className="rounded-xl overflow-hidden mb-3 border border-border bg-muted/20"><img src={post.imageUrl || post.image} alt="Imagem da publicação" className="w-full max-h-[500px] object-cover" /></div>}
      {post.videoUrl && <video controls className="w-full max-h-[500px] rounded-xl mb-3" src={post.videoUrl} />}

      <div className="flex items-center gap-6 pt-2 border-t border-border">
        <button type="button" onClick={() => setShowComments(!showComments)} className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground"><MessageSquare className="h-4 w-4" /><span>{formatNumber(commentsCount)}</span></button>
        <button type="button" onClick={handleRepost} className={`flex items-center gap-1.5 text-xs font-medium ${isReposted ? 'text-green-500' : 'text-muted-foreground hover:text-green-500'}`}><Repeat2 className="h-4 w-4" /><span>{formatNumber(repostsCount)}</span></button>
        <button type="button" onClick={handleLike} className={`flex items-center gap-1.5 text-xs font-medium ${isLiked ? 'text-red-500 font-bold' : 'text-muted-foreground hover:text-red-500'}`}><ArrowUp className={`h-4 w-4 ${isLiked ? 'fill-red-500' : ''}`} /><span>{formatNumber(likesCount)}</span></button>
        <button type="button" onClick={handleShare} className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground ml-auto" aria-label="Compartilhar publicação"><Share className="h-4 w-4" /></button>
      </div>

      {showComments && <div className="mt-4 pt-4 border-t border-border animate-in fade-in slide-in-from-top-2">
        {replyTo && <div className="mb-2 flex items-center justify-between rounded-md bg-muted px-3 py-2 text-xs">Respondendo a um comentário<button type="button" onClick={() => setReplyTo(null)} className="font-semibold underline">Cancelar</button></div>}
        <form onSubmit={submitComment} className="flex gap-2 mb-4">
          <Avatar className="h-8 w-8 shrink-0"><AvatarImage src={getAvatar(loggedUser)} alt="Sua foto" /><AvatarFallback>U</AvatarFallback></Avatar>
          <Input value={newComment} onChange={(event) => setNewComment(event.target.value)} placeholder={replyTo ? 'Escreva uma resposta...' : 'Escreva um comentário...'} className="flex-1 h-9 text-xs bg-muted/30 border-none focus-visible:ring-skillswap-teal" />
          <Button type="submit" size="sm" disabled={!newComment.trim()} className="bg-skillswap-teal hover:bg-teal-700 h-9"><Send size={14} /></Button>
        </form>
        <div className="space-y-4">{commentTree.map((comment) => renderComment(comment))}</div>
      </div>}
    </div>
  );
};

export default PostCard;
