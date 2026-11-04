import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { onIdTokenChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { ArrowLeft, Camera, ImagePlus, Send, Video, Search, UserMinus, ShieldCheck } from 'lucide-react';
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
  const [membershipStatus, setMembershipStatus] = useState('NONE');
  const [members, setMembers] = useState<any[]>([]);
  const [pendingMembers, setPendingMembers] = useState<any[]>([]);
  const [memberSearch, setMemberSearch] = useState('');
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
          try {
            const membershipResponse = await CommunityService.getMembership(communityId);
            setMembershipStatus(String(membershipResponse.data || 'NONE'));
          } catch { setMembershipStatus('NONE'); }
        } catch { setMe(null); setMembershipStatus('NONE'); }
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
      toast.success(community?.privateCommunity ? 'Solicitação enviada para aprovação.' : 'Você entrou na comunidade.');
      await load();
    } catch { toast.error('Não foi possível entrar agora.'); }
  });

  const loadMembers = async () => {
    if (!communityId) return;
    try {
      const [membersResponse, pendingResponse] = await Promise.all([
        CommunityService.getMembers(communityId, memberSearch),
        CommunityService.getPendingMembers(communityId, memberSearch),
      ]);
      setMembers(Array.isArray(membersResponse.data) ? membersResponse.data : []);
      setPendingMembers(Array.isArray(pendingResponse.data) ? pendingResponse.data : []);
    } catch { setMembers([]); }
  };

  useEffect(() => {
    if (membershipStatus === 'ADMIN' || membershipStatus === 'OWNER') void loadMembers();
  }, [membershipStatus, communityId]);

  const approveMember = async (memberId: string) => {
    try {
      await CommunityService.approveMember(communityId!, memberId);
      toast.success('Solicitação aprovada.');
      await loadMembers();
      await load();
    } catch { toast.error('Não foi possível aprovar esta solicitação.'); }
  };

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
  const isModerator = isOwner || membershipStatus === 'ADMIN';
  const isApprovedMember = isOwner || membershipStatus === 'APPROVED' || membershipStatus === 'OWNER' || membershipStatus === 'ADMIN';
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
          {isOwner ? <span className="rounded-full bg-skillswap-teal/10 px-4 py-2 text-sm font-semibold text-skillswap-teal">Criador da comunidade</span> : isApprovedMember ? <span className="rounded-full bg-skillswap-teal/10 px-4 py-2 text-sm font-semibold text-skillswap-teal">Membro</span> : membershipStatus === 'PENDING' ? <span className="rounded-full bg-amber-100 px-4 py-2 text-sm font-semibold text-amber-800">Aguardando aprovação</span> : <Button type="button" onClick={join} className="bg-skillswap-teal text-white">Entrar</Button>}
        </div>
      </section>
      {isApprovedMember && <section className="rounded-2xl border border-border bg-card p-4 sm:p-6">
        <h2 className="font-bold text-lg mb-3">Publicar na comunidade</h2>
        <form onSubmit={publish} className="space-y-3">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Título (opcional)" maxLength={255} />
          <textarea value={content} onChange={(e) => setContent(e.target.value)} placeholder="Compartilhe algo com a comunidade..." className="min-h-28 w-full rounded-lg border border-input bg-background p-3 text-sm" maxLength={1000} required />
          <div className="flex flex-wrap items-center gap-2"><input type="file" accept="image/*,video/*" onChange={(e) => setMedia(e.target.files?.[0] ?? null)} className="max-w-full text-sm" /><Button type="submit" disabled={sending || !content.trim()} className="bg-skillswap-teal text-white"><Send className="mr-2 h-4 w-4" /> {sending ? 'Publicando...' : 'Publicar'}</Button></div>
          {media && <p className="text-xs text-muted-foreground flex items-center gap-1">{media.type.startsWith('video/') ? <Video className="h-3 w-3" /> : <ImagePlus className="h-3 w-3" />} {media.name}</p>}
        </form>
      </section>}
      {isModerator && <section className="rounded-2xl border border-border bg-card p-4 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-bold text-lg flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-skillswap-teal" /> Membros da comunidade</h2>
          <form className="flex gap-2" onSubmit={(event) => { event.preventDefault(); void loadMembers(); }}>
            <Input value={memberSearch} onChange={(event) => setMemberSearch(event.target.value)} placeholder="Pesquisar usuário" className="w-48" />
            <Button type="submit" variant="outline" aria-label="Pesquisar membros"><Search className="h-4 w-4" /></Button>
          </form>
        </div>
        <div className="mt-4 space-y-2">
          {members.map((member) => <div key={member.userId} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border p-3"><div><strong>{member.name || member.username}</strong><span className="ml-2 text-sm text-muted-foreground">@{member.username}</span><span className="ml-2 text-xs text-skillswap-teal">{member.role}</span></div><div className="flex gap-2">{isOwner && member.role !== 'OWNER' && <Button size="sm" variant="outline" onClick={() => CommunityService.setAdministrator(communityId!, member.userId, member.role !== 'ADMIN').then(loadMembers)}>{member.role === 'ADMIN' ? 'Remover admin' : 'Tornar admin'}</Button>}{member.role !== 'OWNER' && <Button size="sm" variant="outline" className="text-red-600" onClick={() => CommunityService.removeMember(communityId!, member.userId).then(loadMembers)}><UserMinus className="mr-1 h-4 w-4" /> Remover</Button>}</div></div>)}
          {!members.length && <p className="text-sm text-muted-foreground">Pesquise ou carregue os membros para administrar a comunidade.</p>}
        </div>
        {pendingMembers.length > 0 && <div className="mt-6 border-t border-border pt-4"><h3 className="font-semibold">Solicitações pendentes</h3><div className="mt-2 space-y-2">{pendingMembers.map((member) => <div key={member.userId} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3"><div><strong>{member.name || member.username}</strong><span className="ml-2 text-sm text-muted-foreground">@{member.username}</span></div><Button size="sm" onClick={() => void approveMember(member.userId)} className="bg-skillswap-teal text-white">Aprovar</Button></div>)}</div></div>}
      </section>}
      <section className="space-y-4"><h2 className="font-bold text-lg">Publicações</h2>{posts.length ? posts.map((post) => <PostCard key={post.postId ?? post.id} post={{ ...post, author: post.user || post.author || { username: post.username || 'Usuário' } }} />) : <p className="rounded-xl border border-dashed p-6 text-center text-muted-foreground">Ainda não há publicações nesta comunidade.</p>}</section>
    </main>
  );
};

export default CommunityDetailPage;
