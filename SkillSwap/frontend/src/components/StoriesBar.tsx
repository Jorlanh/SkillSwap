import { useEffect, useRef, useState } from 'react';
import { Plus, Upload } from 'lucide-react';
import { StoryService } from '@/services/api';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';

const StoriesBar = () => {
  const [stories, setStories] = useState<any[]>([]);
  const [uploading, setUploading] = useState(false);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const videoInputRef = useRef<HTMLInputElement>(null);

  const load = () => StoryService.getAll().then(({ data }) => setStories(Array.isArray(data) ? data : [])).catch(() => setStories([]));
  useEffect(() => { load(); }, []);

  const upload = async (file: File) => {
    setUploading(true);
    try {
      const form = new FormData();
      form.append('media', file);
      await StoryService.create(form);
      toast.success('Story publicado por 24 horas.');
      await load();
    } catch { toast.error('Não foi possível publicar esse story.'); }
    finally { setUploading(false); }
  };

  return <section aria-label="Stories" className="mb-5 rounded-2xl border border-border bg-card p-3"><div className="flex gap-3 overflow-x-auto pb-1">
    <div className="flex h-20 w-20 shrink-0 flex-col gap-1 items-center justify-center rounded-xl border border-dashed border-skillswap-teal text-skillswap-teal text-xs">
      <Plus className="h-5 w-5" />
      <button type="button" onClick={() => imageInputRef.current?.click()} disabled={uploading} className="hover:underline">Foto</button>
      <button type="button" onClick={() => videoInputRef.current?.click()} disabled={uploading} className="hover:underline">Vídeo</button>
    </div>
    <input ref={imageInputRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])} />
    <input ref={videoInputRef} type="file" accept="video/*" capture="environment" className="hidden" onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])} />
    {stories.map((story) => <div key={story.storyId} className="h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-muted" title={`Story de @${story.username}`}>
      {story.mediaType === 'video' ? <video src={story.mediaUrl} className="h-full w-full object-cover" muted playsInline controls /> : <img src={story.mediaUrl} alt={`Story de ${story.username}`} className="h-full w-full object-cover" />}
    </div>)}
    {!stories.length && <p className="flex items-center gap-2 text-sm text-muted-foreground"><Upload className="h-4 w-4" /> Publique uma foto ou vídeo para começar.</p>}
  </div></section>;
};

export default StoriesBar;
