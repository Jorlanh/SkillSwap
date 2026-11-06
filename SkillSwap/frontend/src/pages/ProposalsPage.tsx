import { useEffect, useMemo, useState } from 'react';
import { ProposalService, UserService } from '@/services/api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { ChevronLeft, Check, X, Ban, FileText, Send, MessageCircle } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Textarea } from '@/components/ui/textarea';

const ProposalsPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [proposals, setProposals] = useState<any[]>([]);
  const [selectedProposal, setSelectedProposal] = useState<any>(null);
  const [myUserId, setMyUserId] = useState<string>('');
  const [counterMessage, setCounterMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const proposalId = (proposal: any) => proposal?.proposalId ?? proposal?.id;

  const getOtherParty = (proposal: any) => {
    const senderId = proposal?.sender?.userId || proposal?.sender?.id;
    return String(senderId) === String(myUserId) ? proposal?.receiver : proposal?.sender;
  };

  const proposalSummary = (proposal: any) => {
    const offered = proposal?.offeredSkill?.name || proposal?.offeredSkill?.title;
    const requested = proposal?.requestedSkill?.name || proposal?.requestedSkill?.title;
    if (offered && requested) return `Oferece ${offered} e procura ${requested}`;
    if (offered) return `Oferece ${offered}`;
    if (requested) return `Procura ${requested}`;
    return 'Proposta de troca de conhecimento';
  };

  const isReceiver = useMemo(() => {
    if (!selectedProposal || !myUserId) return false;
    return String(selectedProposal.receiver?.userId || selectedProposal.receiver?.id) === String(myUserId);
  }, [myUserId, selectedProposal]);

  const isSender = useMemo(() => {
    if (!selectedProposal || !myUserId) return false;
    return String(selectedProposal.sender?.userId || selectedProposal.sender?.id) === String(myUserId);
  }, [myUserId, selectedProposal]);

  const fetchProposals = async () => {
    try {
      const [{ data: currentUser }, { data }] = await Promise.all([
        UserService.getMe(),
        ProposalService.getAll(),
      ]);
      const nextProposals = Array.isArray(data) ? data : [];
      const currentId = currentUser?.userId || currentUser?.id || '';
      setMyUserId(String(currentId));
      setProposals(nextProposals);
      setSelectedProposal((current: any) => {
        const currentId = proposalId(current);
        return nextProposals.find((proposal) => proposalId(proposal) === currentId) || nextProposals[0] || null;
      });
    } catch (error) {
      toast.error('Não foi possível carregar suas propostas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchProposals();
  }, []);

  useEffect(() => {
    if ((location.state as any)?.proposalSent) {
      toast.success('Proposta enviada! A pessoa já pode ver sua mensagem nas solicitações.');
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.pathname, location.state, navigate]);

  const handleAction = async (action: 'accept' | 'reject' | 'block') => {
    const id = proposalId(selectedProposal);
    if (!id || isSubmitting) return;

    setIsSubmitting(true);
    try {
      if (action === 'accept') {
        await ProposalService.accept(id);
        const otherParty = getOtherParty(selectedProposal);
        toast.success('Proposta aceita! Abrindo a conversa.');
        navigate('/mensagens', { state: { openChatWith: otherParty } });
        return;
      }

      if (action === 'reject') {
        await ProposalService.reject(id);
        toast.info('Proposta recusada.');
      } else {
        await ProposalService.block(id);
        toast.success('Usuário bloqueado para novas propostas.');
      }
      await fetchProposals();
    } catch (error: any) {
      const data = error?.response?.data;
      const message = typeof data === 'string' ? data : data?.message || data?.error;
      toast.error(message || 'Não foi possível processar esta proposta.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleNegotiate = async () => {
    const id = proposalId(selectedProposal);
    if (!id || !counterMessage.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      await ProposalService.negotiate(id, counterMessage.trim());
      setCounterMessage('');
      toast.success('Contraproposta enviada para a outra pessoa.');
      await fetchProposals();
    } catch (error: any) {
      const data = error?.response?.data;
      const message = typeof data === 'string' ? data : data?.message || data?.error;
      toast.error(message || 'Não foi possível enviar a contraproposta.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return <div className="p-20 text-center font-bold uppercase">Sincronizando propostas...</div>;

  return (
    <div className="max-w-5xl mx-auto p-3 sm:p-4 pb-24">
      <Link to="/" className="inline-flex items-center gap-1 font-bold mb-4 hover:opacity-80 uppercase text-xs">
        <ChevronLeft className="h-4 w-4" /> Voltar ao Feed
      </Link>

      <div className="flex flex-wrap items-center gap-2 mb-6">
        <FileText className="h-6 w-6" />
        <h1 className="font-bold text-xl sm:text-2xl">Minhas Solicitações</h1>
        <span className="bg-skillswap-teal text-white text-xs font-bold px-2 py-1 rounded-full">{proposals.length}</span>
      </div>

      {proposals.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-8 text-center text-muted-foreground">
          Você ainda não recebeu nem enviou propostas.
        </div>
      ) : (
        <div className="flex flex-col md:flex-row gap-4 sm:gap-6">
          <div className="w-full md:w-80 shrink-0 space-y-2">
            {proposals.map((proposal) => {
              const otherParty = getOtherParty(proposal);
              return (
                <button
                  type="button"
                  key={proposalId(proposal)}
                  onClick={() => setSelectedProposal(proposal)}
                  className={`w-full flex items-center justify-between p-3 sm:p-4 rounded-xl text-left border transition-all ${proposalId(selectedProposal) === proposalId(proposal) ? 'bg-skillswap-teal/10 border-skillswap-teal shadow-md' : 'hover:bg-muted border-transparent'}`}
                >
                  <span className="flex items-center gap-3 flex-1 min-w-0">
                    <Avatar className="h-10 w-10 shrink-0"><AvatarImage src={otherParty?.avatarUrl} /><AvatarFallback>{otherParty?.username?.[0]?.toUpperCase() || '?'}</AvatarFallback></Avatar>
                    <span className="min-w-0">
                      <span className="block text-sm font-bold truncate">{otherParty?.username || 'Usuário'}</span>
                      <span className="block text-[10px] text-muted-foreground truncate font-bold uppercase">{proposal.status || 'PENDING'}</span>
                    </span>
                  </span>
                </button>
              );
            })}
          </div>

          {selectedProposal && (
            <div className="flex-1 min-w-0 bg-card rounded-2xl p-4 sm:p-6 lg:p-8 border shadow-xl relative overflow-hidden">
              {(() => {
                const otherParty = getOtherParty(selectedProposal);
                const status = selectedProposal.status || 'PENDING';
                const canActAsReceiver = isReceiver && status === 'PENDING';
                const canAcceptCounter = isSender && status === 'NEGOTIATING';
                return (
                  <>
                    <div className="flex flex-col items-center mb-6">
                      <Avatar className="h-20 w-20 sm:h-24 sm:w-24 mb-3 border-4 border-skillswap-teal"><AvatarImage src={otherParty?.avatarUrl} /><AvatarFallback>{otherParty?.username?.[0]?.toUpperCase() || '?'}</AvatarFallback></Avatar>
                      <h2 className="font-bold text-xl sm:text-2xl break-all text-center">@{otherParty?.username || 'usuário'}</h2>
                      <p className="text-xs font-bold text-muted-foreground uppercase mt-1">{isReceiver ? 'Proposta recebida' : 'Proposta enviada'}</p>
                    </div>

                    <div className="bg-muted/30 p-4 sm:p-6 rounded-2xl mb-4 border border-dashed border-skillswap-teal space-y-3">
                      <p className="text-sm font-semibold text-center">{proposalSummary(selectedProposal)}</p>
                      <div className="rounded-xl bg-background/80 p-3 text-sm break-words whitespace-pre-wrap">
                        <p className="text-[10px] uppercase font-bold text-muted-foreground mb-1">Mensagem da proposta</p>
                        {selectedProposal.message || 'Nenhuma mensagem foi adicionada.'}
                      </div>
                      {selectedProposal.counterMessage && (
                        <div className="rounded-xl bg-skillswap-teal/10 p-3 text-sm break-words whitespace-pre-wrap border border-skillswap-teal/30">
                          <p className="text-[10px] uppercase font-bold text-skillswap-teal mb-1">Contraproposta</p>
                          {selectedProposal.counterMessage}
                        </div>
                      )}
                    </div>

                    <div className="mb-6 rounded-full bg-muted px-3 py-2 text-center text-xs font-bold uppercase tracking-wide">
                      Status: {status === 'NEGOTIATING' ? 'em negociação' : status.toLowerCase()}
                    </div>

                    {(canActAsReceiver || canAcceptCounter) && (
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 sm:gap-3">
                        <Button disabled={isSubmitting} onClick={() => void handleAction('accept')} className="w-full bg-skillswap-accept text-white hover:opacity-90 h-11 font-bold uppercase text-xs"><Check className="mr-2 h-4 w-4" />Aceitar</Button>
                        <Button disabled={isSubmitting} onClick={() => void handleAction('reject')} className="w-full bg-skillswap-reject text-white hover:opacity-90 h-11 font-bold uppercase text-xs"><X className="mr-2 h-4 w-4" />Recusar</Button>
                        {canActAsReceiver && <Button disabled={isSubmitting} onClick={() => void handleAction('block')} variant="outline" className="w-full h-11 border-red-500 text-red-500 hover:bg-red-50 font-bold uppercase text-xs"><Ban className="mr-2 h-4 w-4" />Bloquear</Button>}
                      </div>
                    )}

                    {status === 'ACCEPTED' && (
                      <Button className="w-full bg-skillswap-teal text-white h-11 font-bold" onClick={() => navigate('/mensagens', { state: { openChatWith: otherParty } })}>
                        <MessageCircle className="mr-2 h-4 w-4" />Abrir conversa
                      </Button>
                    )}

                    {canActAsReceiver && (
                      <div className="mt-8 pt-6 border-t border-border">
                        <p className="text-[10px] uppercase font-bold text-muted-foreground mb-3">Negociar antes de aceitar</p>
                        <div className="flex flex-col sm:flex-row gap-2">
                          <Textarea value={counterMessage} onChange={(event) => setCounterMessage(event.target.value)} placeholder="Digite uma contraproposta..." className="min-h-12 flex-1 resize-y bg-muted/30" />
                          <Button disabled={!counterMessage.trim() || isSubmitting} onClick={() => void handleNegotiate()} className="w-full sm:w-auto h-12 bg-skillswap-teal text-white"><Send className="mr-2 h-4 w-4" />Enviar</Button>
                        </div>
                      </div>
                    )}

                    {status === 'NEGOTIATING' && isReceiver && (
                      <p className="mt-6 text-center text-xs text-muted-foreground">A contraproposta foi enviada. Aguarde a resposta da outra pessoa.</p>
                    )}
                    {status === 'PENDING' && isSender && (
                      <p className="mt-6 text-center text-xs text-muted-foreground">Aguardando a resposta da outra pessoa.</p>
                    )}
                  </>
                );
              })()}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ProposalsPage;
