import { useEffect, useState } from 'react';
import { ProposalService } from '@/services/api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { ChevronLeft, CheckCircle2, XCircle, Ban, FileText, Send } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Input } from '@/components/ui/input';

const ProposalsPage = () => {
  const navigate = useNavigate();
  const [proposals, setProposals] = useState<any[]>([]);
  const [selectedProposal, setSelectedProposal] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchProposals = async () => {
    try {
      const { data } = await ProposalService.getAll();
      setProposals(data);
      if (data.length > 0) setSelectedProposal(data[0]);
    } catch (error) {
      toast.error("Erro ao buscar solicitações do banco.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProposals(); }, []);

  const handleAction = async (action: 'accept' | 'reject' | 'block') => {
    try {
      if (action === 'accept') {
        await ProposalService.accept(selectedProposal.id);
        toast.success("Proposta aceita! Abrindo chat...");
        navigate('/mensagens'); 
      } else if (action === 'reject') {
        await ProposalService.reject(selectedProposal.id);
        toast.info("Recusado. Inicie uma negociação no chat abaixo.");
      } else {
        await ProposalService.block(selectedProposal.id);
        toast.error("Usuário bloqueado.");
        fetchProposals();
      }
    } catch (error) {
      toast.error("Falha ao processar ação.");
    }
  };

  if (loading) return <div className="p-20 text-center font-bold uppercase">Sincronizando Banco...</div>;

  return (
    <div className="max-w-5xl mx-auto p-4">
      <Link to="/" className="inline-flex items-center gap-1 font-bold mb-4 hover:opacity-80 uppercase text-xs">
        <ChevronLeft className="h-4 w-4" /> Voltar ao Feed
      </Link>

      <div className="flex items-center gap-2 mb-6">
        <FileText className="h-6 w-6" />
        <h1 className="font-bold text-2xl">Minhas Solicitações</h1>
        <span className="bg-skillswap-teal text-white text-xs font-bold px-2 py-1 rounded-full">{proposals.length}</span>
      </div>

      <div className="flex gap-6">
        <div className="w-80 shrink-0 space-y-2">
          {proposals.map((p) => (
            <div key={p.id} onClick={() => setSelectedProposal(p)} className={`flex items-center justify-between p-4 rounded-xl cursor-pointer border transition-all ${selectedProposal?.id === p.id ? 'bg-skillswap-teal/10 border-skillswap-teal shadow-md' : 'hover:bg-muted border-transparent'}`}>
              <div className="flex items-center gap-3 flex-1 min-w-0">
                <Avatar className="h-10 w-10"><AvatarImage src={p.sender?.avatarUrl} /></Avatar>
                <div className="min-w-0">
                  <p className="text-sm font-bold">{p.sender?.username}</p>
                  <p className="text-[10px] text-muted-foreground truncate font-bold uppercase">{p.message}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {selectedProposal && (
          <div className="flex-1 bg-card rounded-2xl p-8 border shadow-xl relative overflow-hidden">
            <div className="flex flex-col items-center mb-8">
              <Avatar className="h-24 w-24 mb-4 border-4 border-skillswap-teal"><AvatarImage src={selectedProposal.sender?.avatarUrl} /></Avatar>
              <h2 className="font-bold text-2xl">@{selectedProposal.sender?.username}</h2>
              <p className="text-xs font-bold text-muted-foreground uppercase mt-1">Membro desde {new Date(selectedProposal.sender?.createdAt).toLocaleDateString()}</p>
            </div>

            <div className="bg-muted/30 p-6 rounded-2xl mb-8 border border-dashed border-skillswap-teal italic text-sm text-center">
               "{selectedProposal.message}"
            </div>

            <div className="flex gap-3 justify-center">
              <Button onClick={() => handleAction('accept')} className="bg-skillswap-accept text-white hover:opacity-90 px-8 h-12 font-bold uppercase text-xs">Aceitar</Button>
              <Button onClick={() => handleAction('reject')} className="bg-skillswap-reject text-white hover:opacity-90 px-8 h-12 font-bold uppercase text-xs">Recusar</Button>
              <Button onClick={() => handleAction('block')} variant="outline" className="px-8 h-12 border-red-500 text-red-500 hover:bg-red-50 font-bold uppercase text-xs">Bloquear</Button>
            </div>

            <div className="mt-12 pt-8 border-t border-border">
                <p className="text-[10px] uppercase font-bold text-muted-foreground mb-4">Negociação Direta</p>
                <div className="flex gap-2">
                    <Input placeholder="Escreva uma contra-proposta..." className="bg-muted/30 border-none h-12" />
                    <Button size="icon" className="bg-skillswap-teal h-12 w-12"><Send size={18}/></Button>
                </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProposalsPage;