import { useEffect, useRef, useState } from 'react';
import { HomeService } from '@/services/api';
import PostCard from '@/components/PostCard';
import RightSidebar from '@/components/RightSidebar';
import { BrainCircuit, Eye, Image as ImageIcon, Play, X } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Link, useOutletContext } from 'react-router-dom';
import { auth } from '@/lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';

const FeedPage = () => {
  const [posts, setPosts] = useState<any[]>([]);
  const [trending, setTrending] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const [previewMedia, setPreviewMedia] = useState<{ type: 'image' | 'video'; url: string; alt: string } | null>(null);
  const trendingRef = useRef<HTMLDivElement>(null);
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();

  useEffect(() => onAuthStateChanged(auth, (currentUser) => {
    setIsAuthenticated(Boolean(currentUser));
  }), []);

  useEffect(() => {
    if (isAuthenticated === null) return;
    const loadFeed = async () => {
      try {
        const [trendingResponse, recentResponse] = await Promise.all([
          HomeService.getTrendingPosts('DAY'),
          HomeService.getPosts(isAuthenticated ? 'PERSONALIZED' : 'DATE', 'ALL'),
        ]);
        setTrending(Array.isArray(trendingResponse.data) ? trendingResponse.data.slice(0, 10) : []);
        setPosts(Array.isArray(recentResponse.data) ? recentResponse.data : []);
      } catch (error) {
        console.error("Erro ao conectar com o reator backend:", error);
      } finally {
        setIsLoading(false);
      }
    };
    loadFeed();
  }, [isAuthenticated]);

  useEffect(() => {
    const container = trendingRef.current;
    if (!container || trending.length < 2) return;
    const timer = window.setInterval(() => {
      const firstCard = container.firstElementChild as HTMLElement | null;
      if (!firstCard) return;
      const step = firstCard.getBoundingClientRect().width + 12;
      const atEnd = container.scrollLeft + container.clientWidth >= container.scrollWidth - 8;
      container.scrollTo({ left: atEnd ? 0 : container.scrollLeft + step, behavior: 'smooth' });
    }, 5000);
    return () => window.clearInterval(timer);
  }, [trending]);

  return (
    // AJUSTE: max-w-4xl para centralizar melhor e não espalhar tanto para a direita
    <div className="flex gap-6 max-w-4xl mx-auto p-4 justify-center">
      <div className="flex-1 min-w-0 max-w-2xl"> 
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-display font-bold text-xl text-foreground">Em alta</h2>
          <span className="text-[10px] bg-skillswap-teal/10 text-skillswap-teal px-2 py-1 rounded flex items-center gap-1 font-bold">
            <BrainCircuit size={12} /> ALGORITMO ELOS ATIVO
          </span>
        </div>

        <div ref={trendingRef} className="mb-6 flex gap-3 overflow-x-auto scroll-smooth snap-x pb-2" aria-label="As dez publicações em alta">
          {trending.length ? trending.map((post) => {
            const author = post.user || post.author;
            const imageUrl = post.imageUrl || post.image;
            const videoUrl = post.videoUrl || post.video;
            const communityName = post.communityName || post.community?.name || post.community;
            return <article key={`trend-${post.postId || post.id}`} className="min-w-[280px] max-w-[330px] snap-start overflow-hidden rounded-xl border border-border bg-card shadow-sm">
              {(imageUrl || videoUrl) && <button type="button" className="group relative block h-36 w-full cursor-zoom-in overflow-hidden bg-muted/30 text-left" onClick={() => setPreviewMedia({ type: imageUrl ? 'image' : 'video', url: imageUrl || videoUrl, alt: post.title || 'Mídia da publicação em alta' })} aria-label="Ampliar mídia da publicação em alta">
                {imageUrl ? <img src={imageUrl} alt={post.title || 'Imagem da publicação em alta'} className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-[1.03]" /> : <video muted playsInline preload="metadata" src={videoUrl} className="h-full w-full object-cover" />}
                <span className="absolute bottom-2 right-2 rounded-full bg-black/65 p-2 text-white">{imageUrl ? <ImageIcon className="h-4 w-4" /> : <Play className="h-4 w-4" />}</span>
              </button>}
              <div className="p-3">
                {post.title && <h3 className="line-clamp-2 text-sm font-bold text-foreground">{post.title}</h3>}
                {post.content && <p className="mt-1 line-clamp-3 text-sm text-foreground">{post.content}</p>}
                {author?.username && <p className="mt-2 text-xs text-muted-foreground">Por <Link className="text-skillswap-teal hover:underline" to={`/perfil/${author.username}`}>@{author.username}</Link></p>}
                {communityName && <Link className="mt-1 block truncate text-xs text-skillswap-teal hover:underline" to={`/comunidades/${post.communityId || post.community?.id}`}>Na comunidade: {communityName}</Link>}
                <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-muted-foreground" aria-label="Pontuação em alta">
                  <span>{post.likesCount || 0} curtidas</span><span>{post.commentsCount || 0} comentários</span><span>{post.repostsCount || 0} reposts</span><span>{post.sharesCount || 0} compartilhamentos</span><span className="inline-flex items-center gap-1"><Eye className="h-3 w-3" />{post.viewsCount || 0} views</span>
                </div>
              </div>
            </article>;
          }) : <p className="text-sm text-muted-foreground">Ainda não há publicações em alta.</p>}
        </div>

        <h2 className="mb-3 font-display font-bold text-xl text-foreground">Publicações recentes</h2>

        <div className="space-y-4">
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-64 w-full rounded-xl" />
              ))}
            </div>
          ) : posts.length > 0 ? (
            posts.map((post) => (
              <PostCard 
                key={post.postId || post.id || `${post.createdAt || ''}-${post.content || ''}`}
                post={{
                  ...post,
                  author: post.user || post.author || { name: "Usuário", avatar: "" }
                }} 
              />
            ))
          ) : (
            <div className="text-center py-10 text-muted-foreground">Nenhum post encontrado no radar.</div>
          )}
        </div>
      </div>

      {/* Sidebar lateral fixa à direita */}
      <div className="hidden lg:block w-80">
        <RightSidebar />
      </div>

      {previewMedia && <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/85 p-4 sm:p-8" role="dialog" aria-modal="true" aria-label="Visualização ampliada da publicação em alta" onClick={() => setPreviewMedia(null)}>
        <button type="button" className="absolute right-4 top-4 z-10 rounded-full bg-white/15 p-2 text-white hover:bg-white/25" onClick={() => setPreviewMedia(null)} aria-label="Fechar visualização"><X className="h-6 w-6" /></button>
        <div className="flex max-h-full max-w-6xl items-center justify-center" onClick={(event) => event.stopPropagation()}>
          {previewMedia.type === 'image' ? <img src={previewMedia.url} alt={previewMedia.alt} className="max-h-[90vh] max-w-[95vw] rounded-lg object-contain" /> : <video src={previewMedia.url} controls autoPlay playsInline className="max-h-[90vh] max-w-[95vw] rounded-lg object-contain" />}
        </div>
      </div>}
    </div>
  );
};

export default FeedPage;
