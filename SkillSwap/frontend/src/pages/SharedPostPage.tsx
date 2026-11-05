import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { HomeService } from '@/services/api';
import PostCard from '@/components/PostCard';

const SharedPostPage = () => {
  const { postId } = useParams();
  const [post, setPost] = useState<any>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!postId) return;
    HomeService.getPost(Number(postId))
      .then(({ data }) => setPost(data))
      .catch(() => setError(true));
  }, [postId]);

  return (
    <main className="mx-auto w-full max-w-2xl p-4 sm:p-8">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold">Publicação compartilhada</h1>
        <Link className="text-sm text-skillswap-teal hover:underline" to="/">Ir para o feed</Link>
      </div>
      {error && <p className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">Essa publicação não está disponível.</p>}
      {!error && !post && <p className="text-sm text-muted-foreground">Carregando publicação...</p>}
      {post && <PostCard post={{ ...post, author: post.user }} />}
    </main>
  );
};

export default SharedPostPage;
