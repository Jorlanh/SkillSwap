import { useEffect, useState } from 'react';
import { BookOpen, CheckCircle2, Clock, Download, GraduationCap } from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { LessonService } from '@/services/api';

type LessonView = {
  lessonId: number;
  teacherName: string;
  studentName: string;
  scheduledTime: string;
  status: string;
  title?: string;
  introduction?: string;
  durationMinutes: number;
  completedAt?: string;
  certificateAvailable: boolean;
};

type Certificate = {
  certificateCode: string;
  studentName: string;
  studentUsername: string;
  teacherName: string;
  title: string;
  introduction: string;
  durationMinutes: number;
  completedAt: string;
};

const escapeHtml = (value: unknown) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

const formatDate = (value?: string) => value
  ? new Date(value).toLocaleString('pt-BR', { dateStyle: 'full', timeStyle: 'short' })
  : 'Data não informada';

const openCertificateForPrint = (certificate: Certificate) => {
  const popup = window.open('', '_blank', 'width=960,height=720');
  if (!popup) throw new Error('O navegador bloqueou a janela do certificado.');

  const hours = (certificate.durationMinutes / 60).toLocaleString('pt-BR', {
    minimumFractionDigits: certificate.durationMinutes % 60 === 0 ? 0 : 1,
    maximumFractionDigits: 2,
  });

  popup.document.write(`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Certificado SkillSwap</title><style>
    @page { size: A4 landscape; margin: 12mm; } * { box-sizing: border-box; }
    body { margin: 0; font-family: Arial, sans-serif; color: #10253f; background: #eef5f8; }
    .certificate { min-height: 185mm; padding: 22mm; border: 5px solid #0d9488; background: white; display: flex; flex-direction: column; justify-content: space-between; text-align: center; }
    .brand { color: #0d9488; letter-spacing: .16em; font-weight: 800; font-size: 18px; } h1 { margin: 18px 0 10px; font-size: 38px; }
    .intro { max-width: 700px; margin: 0 auto; font-size: 18px; line-height: 1.55; }
    .student { margin: 18px auto; font-size: 34px; font-weight: 800; color: #087f78; border-bottom: 2px solid #087f78; padding: 0 18px 8px; display: inline-block; }
    .details { display: flex; justify-content: center; gap: 42px; margin: 22px 0; font-size: 16px; } .detail strong { display: block; font-size: 22px; margin-top: 6px; }
    .footer { display: flex; justify-content: space-between; gap: 20px; border-top: 1px solid #b7cbd0; padding-top: 14px; font-size: 12px; text-align: left; } .code { font-family: monospace; }
    @media print { body { background: white; } .certificate { border-width: 3px; } }
  </style></head><body><main class="certificate"><div><div class="brand">SKILLSWAP</div><h1>Certificado de conclusão</h1>
    <p class="intro">${escapeHtml(certificate.introduction)}</p><div class="student">${escapeHtml(certificate.studentName)}</div>
    <p class="intro">concluiu a atividade <strong>${escapeHtml(certificate.title)}</strong>, realizada com orientação de <strong>${escapeHtml(certificate.teacherName)}</strong>.</p>
    <div class="details"><div class="detail">Carga horária<strong>${escapeHtml(hours)} h</strong></div><div class="detail">Conclusão<strong>${escapeHtml(formatDate(certificate.completedAt))}</strong></div></div></div>
    <div class="footer"><span>Participante: @${escapeHtml(certificate.studentUsername)}</span><span class="code">Código: ${escapeHtml(certificate.certificateCode)}</span><span>SkillSwap</span></div></main>
    <script>window.onload = function () { window.focus(); window.print(); };</script></body></html>`);
  popup.document.close();
};

const CoursesPage = () => {
  const [lessons, setLessons] = useState<LessonView[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyLesson, setBusyLesson] = useState<number | null>(null);

  const loadLessons = async () => {
    try {
      const { data } = await LessonService.getMine();
      setLessons(Array.isArray(data) ? data : []);
    } catch {
      toast.error('Não foi possível carregar suas aulas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadLessons(); }, []);

  const completeLesson = async (lessonId: number) => {
    setBusyLesson(lessonId);
    try { await LessonService.complete(lessonId); toast.success('Aula concluída. O certificado já está disponível.'); await loadLessons(); }
    catch { toast.error('Não foi possível concluir esta aula.'); }
    finally { setBusyLesson(null); }
  };

  const downloadCertificate = async (lessonId: number) => {
    setBusyLesson(lessonId);
    try { const { data } = await LessonService.getCertificate(lessonId); openCertificateForPrint(data as Certificate); }
    catch (error) { toast.error(error instanceof Error ? error.message : 'Não foi possível gerar o certificado.'); }
    finally { setBusyLesson(null); }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div><h1 className="font-display font-bold text-2xl text-foreground">Aulas e certificados</h1><p className="text-muted-foreground mt-2">Conclua uma aula e gere um certificado com seu nome, carga horária e código de verificação.</p></div>
      {loading ? <Card><CardContent className="p-8 text-center text-muted-foreground">Carregando suas aulas...</CardContent></Card> : lessons.length === 0 ? (
        <Card><CardContent className="p-8 text-center"><BookOpen className="mx-auto mb-3 text-skillswap-teal" /><p className="text-muted-foreground">Você ainda não possui aulas agendadas.</p><p className="text-sm text-muted-foreground mt-2">Quando uma aula for marcada com você, ela aparecerá aqui.</p></CardContent></Card>
      ) : <div className="space-y-4">{lessons.map((lesson) => {
        const completed = lesson.status?.toUpperCase() === 'COMPLETED';
        const hours = (lesson.durationMinutes / 60).toLocaleString('pt-BR', { maximumFractionDigits: 2 });
        return <Card key={lesson.lessonId} className="border-border bg-card"><CardHeader className="flex flex-row items-start justify-between gap-4"><div><CardTitle className="flex items-center gap-2"><GraduationCap className="text-skillswap-teal" />{lesson.title || 'Aula de troca de habilidades'}</CardTitle><p className="text-sm text-muted-foreground mt-2">{lesson.introduction || 'Atividade de aprendizagem entre pares.'}</p></div><Badge variant={completed ? 'default' : 'secondary'}>{completed ? 'Concluída' : lesson.status || 'Agendada'}</Badge></CardHeader><CardContent className="space-y-4"><div className="grid gap-3 sm:grid-cols-3 text-sm"><span><b>Professor:</b> {lesson.teacherName}</span><span><b>Aluno:</b> {lesson.studentName}</span><span className="flex items-center gap-1"><Clock size={15} />{hours} h · {formatDate(lesson.scheduledTime)}</span></div><div className="flex flex-wrap gap-3">{!completed && <Button onClick={() => void completeLesson(lesson.lessonId)} disabled={busyLesson === lesson.lessonId} className="gap-2"><CheckCircle2 size={16} />{busyLesson === lesson.lessonId ? 'Concluindo...' : 'Marcar como concluída'}</Button>}{completed && <Button variant="outline" onClick={() => void downloadCertificate(lesson.lessonId)} disabled={busyLesson === lesson.lessonId} className="gap-2"><Download size={16} />{busyLesson === lesson.lessonId ? 'Gerando...' : 'Gerar certificado'}</Button>}</div></CardContent></Card>;
      })}</div>}
    </div>
  );
};

export default CoursesPage;
