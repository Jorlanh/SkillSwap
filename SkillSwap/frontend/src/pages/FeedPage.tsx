import { useEffect, useState } from 'react';
import { HomeService } from '@/services/api';
import PostCard from '@/components/PostCard';
import RightSidebar from '@/components/RightSidebar';
import { BrainCircuit } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Link, useOutletContext } from 'react-router-dom';

const FeedPage = () => {
  const [posts, setPosts] = useState<any[]>([]);
  const [trending, setTrending] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();

  useEffect(() => {
    const loadFeed = async () => {
      try {
        const [trendingResponse, recentResponse] = await Promise.all([
          HomeService.getTrendingPosts('DAY'),
          HomeService.getPosts('DATE', 'ALL'),
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
  }, []);

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

        <div className="mb-6 flex gap-3 overflow-x-auto pb-2 snap-x" aria-label="As dez publicações em alta">
          {trending.length ? trending.map((post) => <div key={`trend-${post.postId || post.id}`} className="min-w-[260px] max-w-[300px] snap-start rounded-xl border border-border bg-card p-3 shadow-sm">
            <p className="line-clamp-3 text-sm font-medium text-foreground">{post.content || post.title}</p>
            {post.user?.username && <p className="mt-2 text-xs text-muted-foreground">Por <Link className="text-skillswap-teal hover:underline" to={`/perfil/${post.user.username}`}>@{post.user.username}</Link></p>}
            {post.communityId && <Link className="mt-1 block text-xs text-skillswap-teal hover:underline" to={`/comunidades/${post.communityId}`}>Ver comunidade: {post.communityName || post.community}</Link>}
            <p className="mt-2 text-xs text-muted-foreground">{(post.likesCount || 0) + (post.commentsCount || 0) + (post.repostsCount || 0) + (post.sharesCount || 0)} interações</p>
          </div>) : <p className="text-sm text-muted-foreground">Ainda não há publicações em alta.</p>}
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
    </div>
  );
};

export default FeedPage;
