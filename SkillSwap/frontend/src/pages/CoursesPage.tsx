import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Award, CheckCircle2, Clock, Printer } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LessonService, UserService } from '@/services/api';
import { toast } from 'sonner';

type LessonParticipant = {
  userId?: string;
  id?: string;
  name?: string;
  username?: string;
};

type Lesson = {
  lessonId: number;
  title?: string;
  scheduledTime: string;
  durationMinutes?: number;
  status: string;
  teacherConfirmed?: boolean;
  studentConfirmed?: boolean;
  completedAt?: string;
  teacher?: LessonParticipant;
  student?: LessonParticipant;
};

const escapeHtml = (value: unknown) => String(value ?? '').replace(/[&<>'"]/g, (character) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;',
}[character] ?? character));

const printCertificate = (certificate: any) => {
  const popup = window.open('', '_blank', 'noopener,noreferrer,width=900,height=700');
  if (!popup) {
    toast.error('O navegador bloqueou a janela do certificado. Permita pop-ups para a SkillSwap.');
    return;
  }
  const date = new Date(certificate.completedAt).toLocaleDateString('pt-BR');
  const hours = (Number(certificate.durationMinutes || 60) / 60).toFixed(1).replace('.', ',');
  popup.document.write(`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Certificado SkillSwap</title><style>
    body{font-family:Arial,sans-serif;background:#f4f7f8;padding:48px;color:#17202a}
    .certificate{max-width:760px;margin:auto;background:#fff;border:12px solid #0d9488;padding:64px;text-align:center;box-shadow:0 10px 30px #0002}
    h1{color:#0d9488;font-size:36px;margin:0 0 24px} h2{font-size:28px;margin:18px 0}
    p{font-size:18px;line-height:1.6}.meta{margin-top:32px;font-size:14px;color:#435}
    @media print{body{background:#fff;padding:0}.certificate{box-shadow:none}}
  </style></head><body><main class="certificate" aria-labelledby="title">
    <h1 id="title">SkillSwap</h1><p>Certificado de conclusão</p>
    <p>Registramos a participação de</p><h2>${escapeHtml(certificate.studentName)}</h2>
    <p>como aluno na aula <strong>${escapeHtml(certificate.lessonTitle)}</strong>, com duração de ${hours} hora(s), com acompanhamento de ${escapeHtml(certificate.teacherName)}.</p>
    <p>O certificado também reconhece a participação de <strong>${escapeHtml(certificate.teacherName)}</strong> como professor e participante da troca de conhecimentos.</p>
    <p class="meta">Concluído em ${date}<br>Identificador: ${escapeHtml(certificate.certificateId)}</p>
    <p class="meta">Comprovante de participação na plataforma SkillSwap. Não substitui diploma ou certificação profissional.</p>
  </main><script>window.onload=()=>window.print()</script></body></html>`);
  popup.document.close();
};

const CoursesPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [reviewLesson, setReviewLesson] = useState<Lesson | null>(null);
  const [reviewStars, setReviewStars] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubject, setReviewSubject] = useState('');
  const [reviewFullName, setReviewFullName] = useState('');
  const [savingReview, setSavingReview] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const lessonsResponse = await LessonService.getMine();
      setLessons(lessonsResponse.data || []);
      try {
        const userResponse = await UserService.getMe();
        setCurrentUser(userResponse.data || null);
      } catch {
        setCurrentUser(null);
      }
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Não foi possível carregar as aulas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openReview = (lesson: Lesson) => {
    setReviewLesson(lesson);
    setReviewStars(5);
    setReviewComment('');
    setReviewSubject(lesson.title || '');
    setReviewFullName('');
  };

  useEffect(() => {
    if (!lessons.length || reviewLesson) return;
    const requestedLessonId = searchParams.get('lessonId');
    const requestedParticipantId = searchParams.get('participantId');
    const target = requestedLessonId
      ? lessons.find((lesson) => String(lesson.lessonId) === requestedLessonId)
      : requestedParticipantId
        ? lessons.find((lesson) => {
          const participantIds = [
            lesson.teacher?.userId || lesson.teacher?.id,
            lesson.student?.userId || lesson.student?.id,
          ];
          return participantIds.some((id) => String(id) === String(requestedParticipantId))
            && lesson.status !== 'COMPLETED';
        })
        : undefined;

    if (!target) return;
    openReview(target);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('lessonId');
    nextParams.delete('participantId');
    setSearchParams(nextParams, { replace: true });
  }, [lessons, reviewLesson, searchParams, setSearchParams]);

  const submitReview = async () => {
    if (!reviewLesson) return;
    if (!reviewSubject.trim() || !reviewFullName.trim()) {
      toast.error('Informe o assunto da aula e o nome completo para o certificado.');
      return;
    }
    setSavingReview(true);
    try {
      await LessonService.review(reviewLesson.lessonId, {
        stars: reviewStars,
        comment: reviewComment.trim(),
        skillSubject: reviewSubject.trim(),
        fullName: reviewFullName.trim(),
      });
      const response = await LessonService.confirmCompletion(reviewLesson.lessonId);
      toast.success(response.data?.readyForCertificate
        ? 'Avaliação registrada. O certificado dos dois participantes foi liberado.'
        : 'Avaliação registrada. A outra pessoa ainda precisa avaliar e confirmar a aula.');
      setReviewLesson(null);
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Não foi possível registrar a avaliação.');
    } finally {
      setSavingReview(false);
    }
  };

  const certificate = async (lessonId: number) => {
    try {
      const response = await LessonService.getCertificate(lessonId);
      printCertificate(response.data);
    } catch {
      toast.error('O certificado só é liberado após a confirmação dos dois participantes e fica disponível para o aluno.');
    }
  };

  return (
    <main className="max-w-4xl mx-auto p-4 pb-20">
      <header className="mb-6">
        <h1 className="font-display font-bold text-3xl text-foreground">Aulas e certificados</h1>
        <p className="text-muted-foreground mt-2">A participação é gratuita. O certificado é um comprovante de conclusão da aula.</p>
      </header>
      {loading ? <p className="text-muted-foreground">Carregando aulas...</p> : lessons.length === 0 ? (
        <Card className="p-8 text-center"><Clock className="mx-auto mb-3 text-muted-foreground" /><p className="text-muted-foreground">Nenhuma aula encontrada.</p></Card>
      ) : (
        <div className="grid gap-4">
          {lessons.map((lesson) => {
            const completed = lesson.status === 'COMPLETED';
  const currentUserId = currentUser?.userId
    || currentUser?.id
    || currentUser?.user?.userId
    || currentUser?.user?.id;
            const teacherId = lesson.teacher?.userId || lesson.teacher?.id;
            const studentId = lesson.student?.userId || lesson.student?.id;
            const currentUserConfirmed = String(currentUserId) === String(teacherId)
              ? Boolean(lesson.teacherConfirmed)
              : String(currentUserId) === String(studentId)
                ? Boolean(lesson.studentConfirmed)
                : false;
            return <Card key={lesson.lessonId} className="p-5">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <h2 className="font-bold text-lg">{lesson.title || 'Aula SkillSwap'}</h2>
                  <p className="text-sm text-muted-foreground">{new Date(lesson.scheduledTime).toLocaleString('pt-BR')} · {lesson.durationMinutes || 60} minutos</p>
                  <p className="text-sm mt-2">Professor: {lesson.teacher?.name || lesson.teacher?.username || 'participante'}</p>
                  <p className="text-sm">Aluno: {lesson.student?.name || lesson.student?.username || 'participante'}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  {completed ? <Button type="button" onClick={() => certificate(lesson.lessonId)} className="bg-skillswap-teal text-white"><Award size={17} className="mr-2" /> Gerar certificado</Button> : <Button type="button" onClick={() => openReview(lesson)} disabled={savingReview || currentUserConfirmed} variant={currentUserConfirmed ? 'outline' : 'default'}><CheckCircle2 size={17} className="mr-2" />{currentUserConfirmed ? 'Avaliação registrada' : 'Encerrar e avaliar'}</Button>}
                </div>
              </div>
              {!completed && <p className="text-xs text-muted-foreground mt-4">A aula só será concluída depois que professor e aluno confirmarem. O certificado será liberado depois disso.</p>}
            </Card>;
          })}
        </div>
      )}
      <p className="mt-6 text-xs text-muted-foreground flex items-center gap-2"><Printer size={14} /> O botão gera uma página acessível para impressão ou salvamento em PDF pelo navegador.</p>

      {reviewLesson && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" role="dialog" aria-modal="true" aria-labelledby="review-title">
          <div className="w-full max-w-lg rounded-2xl bg-background p-5 shadow-2xl max-h-[90dvh] overflow-y-auto">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 id="review-title" className="text-xl font-bold">Encerrar e avaliar a aula</h2>
                <p className="mt-1 text-sm text-muted-foreground">A avaliação será registrada para a outra pessoa. Depois que os dois participantes confirmarem, o certificado será liberado.</p>
              </div>
              <button type="button" className="text-muted-foreground hover:text-foreground" onClick={() => setReviewLesson(null)} aria-label="Fechar">×</button>
            </div>

            <div className="mt-5 space-y-4">
              <label className="block text-sm font-semibold">Assunto da aula
                <input value={reviewSubject} onChange={(event) => setReviewSubject(event.target.value)} maxLength={180} placeholder="Ex.: Introdução à programação" className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 font-normal" />
              </label>
              <label className="block text-sm font-semibold">Nome completo no certificado
                <input value={reviewFullName} onChange={(event) => setReviewFullName(event.target.value)} maxLength={180} placeholder="Digite seu nome completo" className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 font-normal" />
              </label>
              <fieldset>
                <legend className="text-sm font-semibold">Nota para a outra pessoa</legend>
                <div className="mt-2 flex gap-1" aria-label={`${reviewStars} de 5 estrelas`}>
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button type="button" key={star} onClick={() => setReviewStars(star)} className={`text-3xl leading-none ${star <= reviewStars ? 'text-yellow-400' : 'text-muted-foreground/30'}`} aria-label={`${star} estrelas`}>★</button>
                  ))}
                </div>
              </fieldset>
              <label className="block text-sm font-semibold">Comentário (opcional)
                <textarea value={reviewComment} onChange={(event) => setReviewComment(event.target.value)} maxLength={1200} rows={4} placeholder="Como foi a troca de conhecimentos?" className="mt-1 w-full resize-y rounded-lg border border-border bg-background px-3 py-2 font-normal" />
              </label>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setReviewLesson(null)} disabled={savingReview}>Cancelar</Button>
                <Button type="button" onClick={() => void submitReview()} disabled={savingReview} className="bg-skillswap-teal text-white">{savingReview ? 'Salvando...' : 'Salvar avaliação e encerrar'}</Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};

export default CoursesPage;
