import { useEffect, useState } from 'react';
import { Bell, Clock3, Mail, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { HomeService, SuspensionService } from '@/services/api';
import { toast } from 'sonner';

type BanRecord = {
  id: number;
  scope: string;
  reason: string;
  createdAt?: string;
  expiresAt?: string | null;
};

const SuspensionRecoveryPage = () => {
  const [bans, setBans] = useState<BanRecord[]>([]);
  const [notifications, setNotifications] = useState<any[]>([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    Promise.all([
      SuspensionService.getStatus(),
      HomeService.getUnreadNotifications(),
    ]).then(([status, notificationResponse]) => {
      setBans(status.data?.bans || []);
      setNotifications(notificationResponse.data || []);
    }).catch(() => {
      toast.error('Não foi possível carregar os dados da suspensão.');
    }).finally(() => setLoading(false));
  }, []);

  const submitAppeal = async () => {
    if (!message.trim()) {
      toast.error('Escreva o motivo da contestação.');
      return;
    }
    setSending(true);
    try {
      await SuspensionService.submitAppeal(message.trim());
      setMessage('');
      toast.success('Contestação enviada para a equipe de suporte.');
    } catch {
      toast.error('Não foi possível enviar a contestação agora.');
    } finally {
      setSending(false);
    }
  };

  return (
    <main className="min-h-[calc(100vh-5rem)] bg-background px-4 py-8">
      <div className="mx-auto grid w-full max-w-4xl gap-6 lg:grid-cols-[1.1fr_.9fr]">
        <Card className="border-amber-200 bg-amber-50/70">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-amber-950">
              <ShieldAlert className="h-6 w-6" /> Acesso temporariamente limitado
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5 text-sm text-amber-950">
            <p>
              Sua conta continua acessível para consultar as notificações e enviar uma contestação.
              As demais funções ficam bloqueadas enquanto a suspensão estiver ativa.
            </p>
            <div className="space-y-3">
              {loading ? <p>Carregando informações...</p> : bans.map((ban) => (
                <div key={ban.id} className="rounded-xl border border-amber-200 bg-white p-4">
                  <p className="font-semibold">Escopo: {ban.scope === 'GENERAL' ? 'conta inteira' : ban.scope.toLowerCase()}</p>
                  <p className="mt-1">Motivo: {ban.reason}</p>
                  <p className="mt-1 flex items-center gap-1 text-xs text-amber-800">
                    <Clock3 className="h-3.5 w-3.5" />
                    {ban.expiresAt ? `Válida até ${new Date(ban.expiresAt).toLocaleString('pt-BR')}` : 'Prazo permanente'}
                  </p>
                </div>
              ))}
            </div>
            <div className="space-y-2">
              <label htmlFor="appeal-message" className="font-semibold">Contestar decisão</label>
              <textarea
                id="appeal-message"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                maxLength={4000}
                rows={6}
                placeholder="Explique por que você acredita que a suspensão deve ser revisada."
                className="w-full rounded-xl border border-amber-300 bg-white px-3 py-2 text-foreground outline-none focus:ring-2 focus:ring-amber-500"
              />
              <Button type="button" onClick={() => void submitAppeal()} disabled={sending} className="bg-amber-700 text-white hover:bg-amber-800">
                <Mail className="mr-2 h-4 w-4" /> {sending ? 'Enviando...' : 'Enviar contestação'}
              </Button>
              <p className="text-xs text-amber-800">A mensagem será encaminhada à equipe de suporte para análise.</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Bell className="h-5 w-5 text-skillswap-teal" /> Notificações</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {notifications.length === 0 ? (
              <p className="text-sm text-muted-foreground">Nenhuma notificação nova.</p>
            ) : notifications.map((notification: any) => (
              <div key={notification.notificationId || notification.id} className="rounded-xl border border-border p-3">
                <p className="text-sm">{notification.message || notification.content}</p>
                {(notification.sentAt || notification.createdAt) && (
                  <p className="mt-1 text-xs text-muted-foreground">{new Date(notification.sentAt || notification.createdAt).toLocaleString('pt-BR')}</p>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </main>
  );
};

export default SuspensionRecoveryPage;
