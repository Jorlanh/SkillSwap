import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, CheckCircle2, Clock3, Video } from 'lucide-react';
import { Link } from 'react-router-dom';
import { LessonService, UserService } from '@/services/api';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';

const formatDate = (value: string) => new Date(value).toLocaleString('pt-BR', {
  dateStyle: 'full',
  timeStyle: 'short',
});

const AgendaPage = () => {
  const [lessons, setLessons] = useState<any[]>([]);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const loadAgenda = async () => {
    setLoading(true);
    try {
      const { data: lessonData } = await LessonService.getMine();
      setLessons(Array.isArray(lessonData) ? lessonData : []);
      try {
        const { data: userData } = await UserService.getMe();
        setCurrentUser(userData || null);
      } catch {
        setCurrentUser(null);
      }
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Não foi possível carregar sua agenda agora.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadAgenda(); }, []);

  const orderedLessons = useMemo(() => [...lessons].sort((a, b) => new Date(a.scheduledTime || 0).getTime() - new Date(b.scheduledTime || 0).getTime()), [lessons]);

  return (
    <section className="mx-auto w-full max-w-5xl space-y-5 p-2 sm:p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-sm font-semibold text-skillswap-teal">SkillSwap</p><h1 className="text-2xl font-bold">Agenda</h1><p className="mt-1 text-sm text-muted-foreground">Aulas e chamadas confirmadas ficam registradas aqui.</p></div>
        <CalendarDays className="h-10 w-10 text-skillswap-teal" aria-hidden="true" />
      </div>
      {loading ? <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">Carregando sua agenda...</div> : orderedLessons.length === 0 ? <div className="rounded-xl border border-dashed border-border bg-card p-10 text-center text-sm text-muted-foreground">Nenhuma aula ou chamada agendada ainda.</div> : <div className="space-y-3">
        {orderedLessons.map((lesson) => {
          const currentUserId = currentUser?.userId
            || currentUser?.id
            || currentUser?.user?.userId
            || currentUser?.user?.id;
          const teacherId = lesson.teacher?.userId || lesson.teacher?.id;
          const studentId = lesson.student?.userId || lesson.student?.id;
          const other = String(teacherId) === String(currentUserId) ? lesson.student : lesson.teacher;
          const otherId = other?.userId || other?.id;
          const room = otherId && currentUserId ? `skillswap-call-${[String(currentUserId), String(otherId)].sort().join('-')}` : '';
          return <article key={lesson.lessonId || lesson.id} className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="font-bold">{lesson.title || 'Aula SkillSwap'}</h2><p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground"><Clock3 className="h-4 w-4" />{lesson.scheduledTime ? formatDate(lesson.scheduledTime) : 'Data não informada'}</p><p className="mt-1 text-sm text-muted-foreground">Com {other?.name || other?.username || 'participante'}</p></div><span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700"><CheckCircle2 className="h-4 w-4" />{lesson.status || 'AGENDADA'}</span></div>
            <div className="mt-4 flex flex-wrap gap-2">{otherId && room && <Link to={`/chamada/${otherId}?room=${encodeURIComponent(room)}&peerName=${encodeURIComponent(other?.name || other?.username || '')}&peerUsername=${encodeURIComponent(other?.username || '')}&peerAvatar=${encodeURIComponent(other?.profile?.imageUrl || other?.avatarUrl || other?.imageUrl || '')}`}><Button className="bg-skillswap-teal text-white"><Video className="mr-2 h-4 w-4" />Entrar na chamada</Button></Link>}<Button type="button" variant="outline" onClick={() => void loadAgenda()}>Atualizar</Button></div>
          </article>;
        })}
      </div>}
    </section>
  );
};

export default AgendaPage;
