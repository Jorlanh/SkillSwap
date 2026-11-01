import { useEffect, useState } from 'react';
import { Award, CheckCircle2, Clock, Printer } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LessonService } from '@/services/api';
import { toast } from 'sonner';

type Lesson = {
  lessonId: number;
  title?: string;
  scheduledTime: string;
  durationMinutes?: number;
  status: string;
  teacherConfirmed?: boolean;
  studentConfirmed?: boolean;
  completedAt?: string;
  teacher?: { name?: string; username?: string };
  student?: { name?: string; username?: string };
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
    <p>Declaramos que</p><h2>${escapeHtml(certificate.studentName)}</h2>
    <p>concluiu a aula <strong>${escapeHtml(certificate.lessonTitle)}</strong>, com duração de ${hours} hora(s), com acompanhamento de ${escapeHtml(certificate.teacherName)}.</p>
    <p class="meta">Concluído em ${date}<br>Identificador: ${escapeHtml(certificate.certificateId)}</p>
    <p class="meta">Comprovante de participação na plataforma SkillSwap. Não substitui diploma ou certificação profissional.</p>
  </main><script>window.onload=()=>window.print()</script></body></html>`);
  popup.document.close();
};

const CoursesPage = () => {
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const response = await LessonService.getMine();
      setLessons(response.data || []);
    } catch {
      toast.error('Não foi possível carregar as aulas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const confirm = async (lessonId: number) => {
    setWorkingId(lessonId);
    try {
      const response = await LessonService.confirmCompletion(lessonId);
      toast.success(response.data.readyForCertificate
        ? 'Aula concluída. O certificado foi liberado para o aluno.'
        : 'Sua confirmação foi registrada. A outra pessoa ainda precisa confirmar.');
      await load();
    } catch {
      toast.error('Não foi possível confirmar a conclusão.');
    } finally {
      setWorkingId(null);
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
            const currentUserConfirmed = lesson.studentConfirmed || lesson.teacherConfirmed;
            return <Card key={lesson.lessonId} className="p-5">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <h2 className="font-bold text-lg">{lesson.title || 'Aula SkillSwap'}</h2>
                  <p className="text-sm text-muted-foreground">{new Date(lesson.scheduledTime).toLocaleString('pt-BR')} · {lesson.durationMinutes || 60} minutos</p>
                  <p className="text-sm mt-2">Professor: {lesson.teacher?.name || lesson.teacher?.username || 'participante'}</p>
                  <p className="text-sm">Aluno: {lesson.student?.name || lesson.student?.username || 'participante'}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  {completed ? <Button type="button" onClick={() => certificate(lesson.lessonId)} className="bg-skillswap-teal text-white"><Award size={17} className="mr-2" /> Gerar certificado</Button> : <Button type="button" onClick={() => confirm(lesson.lessonId)} disabled={workingId === lesson.lessonId} variant={currentUserConfirmed ? 'outline' : 'default'}><CheckCircle2 size={17} className="mr-2" />{currentUserConfirmed ? 'Confirmação registrada' : 'Concluir aula'}</Button>}
                </div>
              </div>
              {!completed && <p className="text-xs text-muted-foreground mt-4">A aula só será concluída depois que professor e aluno confirmarem. O certificado será liberado depois disso.</p>}
            </Card>;
          })}
        </div>
      )}
      <p className="mt-6 text-xs text-muted-foreground flex items-center gap-2"><Printer size={14} /> O botão gera uma página acessível para impressão ou salvamento em PDF pelo navegador.</p>
    </main>
  );
};

export default CoursesPage;
