import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { onIdTokenChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { ArrowLeft, Camera, ImagePlus, Send, Video } from 'lucide-react';
import { CommunityService, UserService } from '@/services/api';
import PostCard from '@/components/PostCard';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';

const CommunityDetailPage = () => {
  const { communityId } = useParams();
  const navigate = useNavigate();
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();
  const [community, setCommunity] = useState<any>(null);
  const [posts, setPosts] = useState<any[]>([]);
  const [content, setContent] = useState('');
  const [title, setTitle] = useState('');
  const [media, setMedia] = useState<File | null>(null);
  const [me, setMe] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const load = async () => {
    if (!communityId) return;
    setLoading(true);
    try {
      const [communityResponse, postsResponse] = await Promise.all([
        CommunityService.getById(communityId),
        CommunityService.getPosts(communityId),
      ]);
      setCommunity(communityResponse.data);
      setPosts(Array.isArray(postsResponse.data) ? postsResponse.data : []);
      if (auth.currentUser) {
        try {
          const meResponse = await UserService.getMe();
          setMe(meResponse.data);
        } catch { setMe(null); }
      } else {
        setMe(null);
      }
    } catch {
      toast.error('Não foi possível carregar esta comunidade.');
    } finally { setLoading(false); }
  };

  useEffect(() => {
    const unsubscribe = onIdTokenChanged(auth, () => { void load(); });
    return unsubscribe;
  }, [communityId]);

  const join = () => requireAuth(async () => {
    try {
      await CommunityService.join(communityId!);
      toast.success('Você entrou na comunidade.');
      await load();
    } catch { toast.error('Não foi possível entrar agora.'); }
  });

  const publish = (event: React.FormEvent) => {
    event.preventDefault();
    if (!content.trim() || !communityId) return;
    requireAuth(async () => {
      setSending(true);
      try {
        const form = new FormData();
        form.append('title', title.trim());
        form.append('content', content.trim());
        if (media) form.append(media.type.startsWith('video/') ? 'video' : 'image', media);
        await CommunityService.createPost(communityId, form);
        setTitle(''); setContent(''); setMedia(null);
        toast.success('Publicação feita na comunidade.');
        await load();
      } catch { toast.error('Não foi possível publicar agora.'); }
      finally { setSending(false); }
    });
  };

  const updateAvatar = (file: File) => requireAuth(async () => {
    try {
      await CommunityService.updateAvatar(communityId!, file);
      toast.success('Foto da comunidade atualizada.');
      await load();
    } catch { toast.error('Somente quem criou a comunidade pode alterar a foto.'); }
  });

  if (loading) return <main className="max-w-4xl mx-auto p-6 text-muted-foreground">Carregando comunidade...</main>;
  if (!community) return <main className="max-w-4xl mx-auto p-6">Comunidade não encontrada.</main>;

  const currentUserId = me?.userId ?? me?.id;
  const ownerId = community.ownerId ?? community.owner?.userId ?? community.owner?.id;
  const isOwner = Boolean(currentUserId && ownerId && String(currentUserId) === String(ownerId));
  return (
    <main className="max-w-4xl mx-auto p-4 space-y-4">
      <Link to="/comunidades" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="h-4 w-4" /> Voltar para comunidades</Link>
      <section className="rounded-2xl border border-border bg-card p-5 sm:p-7">
        <div className="flex flex-col sm:flex-row gap-4 sm:items-center">
          <div className="relative shrink-0">
            {community.imageUrl ? <img src={community.imageUrl} alt={`Imagem da comunidade ${community.name}`} className="h-24 w-24 rounded-2xl object-cover" /> : <div className="h-24 w-24 rounded-2xl bg-skillswap-light-blue grid place-items-center text-3xl font-bold">{community.name?.slice(0, 1).toUpperCase()}</div>}
            {isOwner && <button type="button" aria-label="Alterar foto da comunidade" onClick={() => fileRef.current?.click()} className="absolute -bottom-2 -right-2 rounded-full bg-skillswap-teal p-2 text-white"><Camera className="h-4 w-4" /></button>}
            {isOwner && <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={(e) => e.target.files?.[0] && updateAvatar(e.target.files[0])} />}
          </div>
          <div className="min-w-0 flex-1"><h1 className="text-2xl font-bold break-words">{community.name}</h1><p className="mt-2 text-muted-foreground break-words">{community.description || 'Um espaço para aprender e compartilhar.'}</p><p className="mt-2 text-sm text-muted-foreground">{community.memberCount ?? 0} membros</p></div>
          {isOwner ? <span className="rounded-full bg-skillswap-teal/10 px-4 py-2 text-sm font-semibold text-skillswap-teal">Criador da comunidade</span> : <Button type="button" onClick={join} className="bg-skillswap-teal text-white">Entrar</Button>}
        </div>
      </section>
      <section className="rounded-2xl border border-border bg-card p-4 sm:p-6">
        <h2 className="font-bold text-lg mb-3">Publicar na comunidade</h2>
        <form onSubmit={publish} className="space-y-3">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Título (opcional)" maxLength={255} />
          <textarea value={content} onChange={(e) => setContent(e.target.value)} placeholder="Compartilhe algo com a comunidade..." className="min-h-28 w-full rounded-lg border border-input bg-background p-3 text-sm" maxLength={1000} required />
          <div className="flex flex-wrap items-center gap-2"><input type="file" accept="image/*,video/*" onChange={(e) => setMedia(e.target.files?.[0] ?? null)} className="max-w-full text-sm" /><Button type="submit" disabled={sending || !content.trim()} className="bg-skillswap-teal text-white"><Send className="mr-2 h-4 w-4" /> {sending ? 'Publicando...' : 'Publicar'}</Button></div>
          {media && <p className="text-xs text-muted-foreground flex items-center gap-1">{media.type.startsWith('video/') ? <Video className="h-3 w-3" /> : <ImagePlus className="h-3 w-3" />} {media.name}</p>}
        </form>
      </section>
      <section className="space-y-4"><h2 className="font-bold text-lg">Publicações</h2>{posts.length ? posts.map((post) => <PostCard key={post.postId ?? post.id} post={{ ...post, author: post.user || post.author || { username: post.username || 'Usuário' } }} />) : <p className="rounded-xl border border-dashed p-6 text-center text-muted-foreground">Ainda não há publicações nesta comunidade.</p>}</section>
    </main>
  );
};

export default CommunityDetailPage;
