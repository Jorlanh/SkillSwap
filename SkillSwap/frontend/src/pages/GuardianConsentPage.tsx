import { FormEvent, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { PrivacyService } from '@/services/api';

const GuardianConsentPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token') || '';
  const [status, setStatus] = useState<any>(null);
  const [guardianName, setGuardianName] = useState('');
  const [relationship, setRelationship] = useState('');
  const [consentGranted, setConsentGranted] = useState(true);
  const [message, setMessage] = useState('Carregando solicitação...');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) {
      setMessage('O link de confirmação não contém um token válido.');
      return;
    }
    PrivacyService.guardianConsentStatus(token)
      .then(response => {
        setStatus(response.data);
        setMessage('Confira os dados e confirme a solicitação se você for responsável por esta pessoa.');
      })
      .catch(error => setMessage(error?.response?.data?.error || 'Não foi possível validar este link.'));
  }, [token]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      const response = await PrivacyService.confirmGuardianConsent({ token, guardianName, relationship, consentGranted });
      setMessage(response.data.approved
        ? 'Confirmação registrada. A conta da pessoa menor continua disponível no SkillSwap.'
        : 'A solicitação foi recusada.');
      setStatus({ ...status, status: response.data.status });
    } catch (error: any) {
      setMessage(error?.response?.data?.error || 'Não foi possível registrar a confirmação.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-background px-6 py-12 text-foreground">
      <article className="mx-auto max-w-xl space-y-6 rounded-3xl border border-border bg-card p-8 shadow-sm">
        <header>
          <p className="text-sm font-semibold uppercase tracking-wide text-skillswap-teal">SkillSwap</p>
          <h1 className="mt-2 text-3xl font-bold">Confirmação de responsável</h1>
          <p className="mt-3 text-sm text-muted-foreground">Esta página registra a confirmação de um responsável para uma conta de pessoa menor de 18 anos.</p>
        </header>

        {status && (
          <div className="rounded-xl border border-border bg-muted/40 p-4 text-sm" aria-live="polite">
            <p><strong>Conta:</strong> {status.minorName}</p>
            <p><strong>Vínculo informado:</strong> {status.relationship}</p>
            <p><strong>Status:</strong> {status.status}</p>
            <p className="mt-2 text-muted-foreground">A confirmação por e-mail verifica o controle deste endereço. Ela não substitui documentos ou análises jurídicas exigidos pela legislação aplicável.</p>
          </div>
        )}

        <p className="text-sm" role="status" aria-live="polite">{message}</p>

        {status?.status === 'PENDING' && (
          <form onSubmit={submit} className="space-y-4">
            <label className="block text-sm font-medium">Seu nome
              <input className="mt-1 h-11 w-full rounded-lg border border-border bg-background px-3" value={guardianName} onChange={event => setGuardianName(event.target.value)} required />
            </label>
            <label className="block text-sm font-medium">Seu vínculo com a pessoa
              <input className="mt-1 h-11 w-full rounded-lg border border-border bg-background px-3" value={relationship} onChange={event => setRelationship(event.target.value)} required />
            </label>
            <label className="flex items-start gap-3 text-sm">
              <input type="checkbox" checked={consentGranted} onChange={event => setConsentGranted(event.target.checked)} className="mt-1" />
              <span>Confirmo que sou responsável por esta pessoa e autorizo o uso da conta no SkillSwap conforme a política de privacidade.</span>
            </label>
            <button type="submit" disabled={loading || !consentGranted} className="w-full rounded-xl bg-skillswap-teal px-4 py-3 font-semibold text-white disabled:opacity-50">
              {loading ? 'Registrando...' : 'Confirmar responsabilidade'}
            </button>
          </form>
        )}

        <Link to="/privacidade" className="inline-block text-sm text-skillswap-teal underline">Ler a Política de Privacidade</Link>
      </article>
    </main>
  );
};

export default GuardianConsentPage;
