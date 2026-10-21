import { useEffect, useState } from 'react';
import { NotificationService, ProposalService, CommunityService } from '@/services/api'; // CommunityService IMPORTADO
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { 
  Check, 
  X, 
  Ban, 
  Bell, 
  Flame, 
  ChevronRight, 
  MessageSquare, 
  Clock, 
  Handshake 
} from 'lucide-react';
import { useToast } from "@/hooks/use-toast";
import { Skeleton } from '@/components/ui/skeleton';
import { Link } from 'react-router-dom'; // IMPORTADO para navegar ao clicar na comunidade

const ActivitiesPage = () => {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [proposals, setProposals] = useState<any[]>([]);
  const [viralCommunities, setViralCommunities] = useState<any[]>([]); // ESTADO PARA COMUNIDADES
  const [isLoading, setIsLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    loadActivities();
  }, []);

  const loadActivities = async () => {
    setIsLoading(true);
    try {
      // INCREMENTO: Buscando Comunidades em paralelo com Propostas e Notificações
      const [notifRes, propRes, commRes] = await Promise.all([
        NotificationService.getAll(),
        ProposalService.getAll(),
        CommunityService.getAll().catch(() => ({ data: [] })) // Fallback silencioso se falhar
      ]);
      
      setNotifications(notifRes.data || []);
      setProposals(propRes.data || []);
      
      // Pega as 3 principais comunidades para a Sidebar (Você pode ajustar a lógica de ordenação se houver um campo de 'score')
      const topCommunities = Array.isArray(commRes.data) 
        ? commRes.data.slice(0, 3) 
        : [];
      setViralCommunities(topCommunities);
      
    } catch (error) {
      console.error("Falha na telemetria de atividades:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleProposalAction = async (id: number, action: 'accept' | 'reject' | 'block') => {
    try {
      if (action === 'accept') await ProposalService.accept(id);
      else if (action === 'reject') await ProposalService.reject(id);
      else await ProposalService.block(id);

      toast({ 
        title: action === 'accept' ? "Match Confirmado!" : "Ação Concluída",
        description: action === 'accept' ? "O chat foi liberado para vocês." : "A solicitação foi removida."
      });
      
      loadActivities();
    } catch (error) {
      toast({ title: "Erro", description: "Falha ao processar comando.", variant: "destructive" });
    }
  };

  return (
    <div className="flex gap-6 max-w-5xl mx-auto p-4 md:p-0">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-6 text-foreground">
          <Bell className="h-6 w-6 text-skillswap-teal" />
          <h1 className="font-display font-bold text-2xl">Centro de Atividade</h1>
        </div>

        <Tabs defaultValue="proposals" className="w-full">
          <TabsList className="grid w-full grid-cols-2 max-w-[400px] mb-8 bg-muted/50 p-1 rounded-2xl h-12">
            <TabsTrigger value="proposals" className="rounded-xl font-bold flex gap-2">
              Solicitações <span className="bg-skillswap-teal text-white text-[10px] px-2 py-0.5 rounded-full">{proposals.length}</span>
            </TabsTrigger>
            <TabsTrigger value="notifications" className="rounded-xl font-bold">Notificações</TabsTrigger>
          </TabsList>

          <TabsContent value="proposals" className="space-y-4">
            {isLoading ? (
              Array(2).fill(0).map((_, i) => <Skeleton key={i} className="h-32 w-full rounded-3xl" />)
            ) : proposals.length > 0 ? (
              proposals.map((prop) => (
                <Card key={prop.id} className="border-border bg-card shadow-sm rounded-3xl overflow-hidden hover:border-skillswap-teal/50 transition-all">
                  <CardContent className="p-6">
                    <div className="flex flex-col md:flex-row items-center gap-6">
                      <Avatar className="w-16 h-16 border-2 border-skillswap-teal">
                        <AvatarImage src={prop.sender?.avatarUrl} />
                        <AvatarFallback>{prop.sender?.username?.[0]}</AvatarFallback>
                      </Avatar>

                      <div className="flex-1 text-center md:text-left">
                        <h3 className="font-bold text-lg">
                          <span className="text-skillswap-teal">@{prop.sender?.username}</span> deseja trocar conhecimentos.
                        </h3>
                        <p className="text-xs text-muted-foreground mt-1">
                          Interesse em aprender: <span className="font-bold text-foreground">{prop.targetSkill}</span>
                        </p>
                        <div className="mt-3 p-3 bg-muted/30 rounded-xl text-xs italic border-l-4 border-skillswap-teal">
                          "{prop.message || "Olá! Vamos trocar habilidades?"}"
                        </div>
                      </div>

                      <div className="flex gap-2 shrink-0">
                        <Button onClick={() => handleProposalAction(prop.id, 'accept')} className="bg-skillswap-teal hover:bg-teal-700 rounded-full w-12 h-12">
                          <Check size={20} />
                        </Button>
                        <Button onClick={() => handleProposalAction(prop.id, 'reject')} variant="outline" className="rounded-full w-12 h-12">
                          <X size={20} />
                        </Button>
                        <Button onClick={() => handleProposalAction(prop.id, 'block')} variant="ghost" className="rounded-full w-12 h-12 text-red-500 hover:bg-red-50">
                          <Ban size={20} />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))
            ) : (
              <div className="text-center py-20 bg-muted/20 rounded-[40px] border-2 border-dashed border-border text-muted-foreground">
                <Handshake className="mx-auto mb-4 opacity-20" size={48} />
                <p className="font-bold">Nenhuma solicitação pendente.</p>
              </div>
            )}
          </TabsContent>

          <TabsContent value="notifications" className="space-y-2">
            {notifications.map((notif) => (
              <div key={notif.id} className={`flex items-center gap-4 p-4 rounded-2xl transition-all ${notif.read ? 'opacity-50' : 'bg-skillswap-teal/5 border-l-4 border-skillswap-teal shadow-sm'}`}>
                <div className="p-2 rounded-full bg-background shadow-sm flex items-center justify-center">
                  {notif.type === 'LIKE' && <span className="text-red-500 text-lg leading-none">❤️</span>}
                  {notif.type === 'COMMENT' && <MessageSquare className="text-blue-500" size={18} />}
                  {notif.type === 'SYSTEM' && <Clock className="text-orange-500" size={18} />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-foreground truncate">{notif.content}</p>
                  <p className="text-[10px] text-muted-foreground uppercase font-bold">{new Date(notif.createdAt).toLocaleDateString()}</p>
                </div>
                {!notif.read && <div className="w-2 h-2 rounded-full bg-skillswap-teal animate-pulse shrink-0" />}
              </div>
            ))}
          </TabsContent>
        </Tabs>
      </div>

      <aside className="hidden xl:block w-72 shrink-0">
        <div className="bg-card rounded-3xl border border-border p-6 sticky top-24 shadow-sm">
          <h3 className="font-display font-bold text-lg text-foreground mb-6 flex items-center gap-2">
            <Flame className="h-5 w-5 text-orange-500 fill-orange-500" /> Comunidades virais
          </h3>
          <div className="space-y-4">
            {isLoading ? (
              // Skeleton Loading para a Sidebar
              [1, 2, 3].map((i) => (
                 <div key={i} className="flex items-center gap-3 p-2">
                    <Skeleton className="w-10 h-10 rounded-lg" />
                    <Skeleton className="h-4 w-32" />
                 </div>
              ))
            ) : viralCommunities.length > 0 ? (
              // Renderizando as Comunidades Reais do Backend
              viralCommunities.map((community, index) => (
                <Link 
                  key={community.id || index} 
                  to="/comunidades" // Roteamento padrão (ajuste se tiver Rota específica da comunidade)
                  className="group flex items-center justify-between p-3 rounded-xl hover:bg-muted transition-colors cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-skillswap-teal/10 flex items-center justify-center font-bold text-skillswap-teal shrink-0">
                      #{index + 1}
                    </div>
                    <span className="text-sm font-bold truncate max-w-[120px]" title={community.name}>
                      {community.name || "Comunidade"}
                    </span>
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform shrink-0" />
                </Link>
              ))
            ) : (
               <div className="text-sm text-muted-foreground text-center py-4">
                  Nenhuma comunidade encontrada.
               </div>
            )}
          </div>
        </div>
      </aside>
    </div>
  );
};

export default ActivitiesPage;