import { useState } from 'react';
import { Flag, Send } from 'lucide-react';
import { ReportService, UserService } from '@/services/api';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';

type ReportButtonProps = {
  reportedUserId?: string;
  targetType: 'USER' | 'COMMUNITY' | 'POST' | 'COMMENT';
  targetId?: string | number;
  label?: string;
};

const categories = [
  ['HATE', 'Discurso de ódio ou discriminação'],
  ['HARASSMENT', 'Assédio ou perseguição'],
  ['VIOLENCE', 'Ameaça ou incentivo à violência'],
  ['SEXUAL', 'Conteúdo sexual inadequado'],
  ['PRIVACY', 'Exposição de dados pessoais'],
  ['SPAM', 'Spam ou golpe'],
  ['OTHER', 'Outro motivo'],
] as const;

const ReportButton = ({ reportedUserId, targetType, targetId, label = 'Denunciar' }: ReportButtonProps) => {
  const [open, setOpen] = useState(false);
  const [category, setCategory] = useState(categories[0][0]);
  const [reason, setReason] = useState('');
  const [sending, setSending] = useState(false);

  const submit = async () => {
    if (!reportedUserId) {
      toast.error('Não foi possível identificar o responsável por este conteúdo.');
      return;
    }
    if (!reason.trim()) {
      toast.error('Explique brevemente o motivo da denúncia.');
      return;
    }
    setSending(true);
    try {
      const { data: currentUser } = await UserService.getMe();
      await ReportService.create({
        reporterId: currentUser.userId,
        reportedUserId,
        targetType,
        targetId: targetId == null ? undefined : String(targetId),
        category,
        reason: reason.trim(),
      });
      toast.success('Denúncia enviada para análise.');
      setReason('');
      setOpen(false);
    } catch {
      toast.error('Não foi possível enviar a denúncia agora.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="relative">
      <Button type="button" size="sm" variant="ghost" className="text-muted-foreground" onClick={() => setOpen((value) => !value)}>
        <Flag className="mr-1 h-4 w-4" /> {label}
      </Button>
      {open && <div className="absolute right-0 z-30 mt-2 w-80 max-w-[calc(100vw-2rem)] rounded-xl border border-border bg-card p-4 shadow-xl">
        <p className="font-semibold">Denunciar {targetType === 'USER' ? 'perfil' : targetType === 'COMMUNITY' ? 'comunidade' : targetType === 'POST' ? 'publicação' : 'comentário'}</p>
        <label className="mt-3 block text-xs font-medium">Motivo<select value={category} onChange={(event) => setCategory(event.target.value)} className="mt-1 w-full rounded-md border border-input bg-background px-2 py-2 text-sm">{categories.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></label>
        <label className="mt-3 block text-xs font-medium">Explique o que aconteceu<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={1000} rows={4} className="mt-1 w-full resize-none rounded-md border border-input bg-background px-2 py-2 text-sm" placeholder="Descreva o motivo da denúncia." /></label>
        <div className="mt-3 flex justify-end gap-2"><Button type="button" variant="outline" size="sm" onClick={() => setOpen(false)}>Cancelar</Button><Button type="button" size="sm" disabled={sending} className="bg-skillswap-teal text-white" onClick={() => void submit()}><Send className="mr-1 h-3 w-3" />Enviar</Button></div>
      </div>}
    </div>
  );
};

export default ReportButton;
