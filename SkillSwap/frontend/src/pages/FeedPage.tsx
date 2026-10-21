import { useEffect, useState } from 'react';
import { HomeService } from '@/services/api';
import PostCard from '@/components/PostCard';
import RightSidebar from '@/components/RightSidebar';
import { BrainCircuit } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { useOutletContext } from 'react-router-dom';

const FeedPage = () => {
  const [posts, setPosts] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { requireAuth } = useOutletContext<{ requireAuth: (action: () => void) => void }>();

  useEffect(() => {
    const loadFeed = async () => {
      try {
        const { data } = await HomeService.getTrending();
        const safeData = Array.isArray(data) ? data : [];
        const sortedPosts = safeData.sort((a: any, b: any) => {
          const scoreA = (a.likesCount || 0) + (a.commentsCount || 0) + (a.sharesCount || 0) + (a.repostsCount || 0);
          const scoreB = (b.likesCount || 0) + (b.commentsCount || 0) + (b.sharesCount || 0) + (b.repostsCount || 0);
          return scoreB - scoreA;
        });
        setPosts(sortedPosts);
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
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display font-bold text-xl text-foreground">Em alta</h2>
          <span className="text-[10px] bg-skillswap-teal/10 text-skillswap-teal px-2 py-1 rounded flex items-center gap-1 font-bold">
            <BrainCircuit size={12} /> ALGORITMO ELOS ATIVO
          </span>
        </div>

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
                key={post.postId || post.id || Math.random()} 
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