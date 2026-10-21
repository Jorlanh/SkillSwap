import { useEffect, useState, useRef } from 'react';
import { ChatService, BASE_URL, RatingService, UserService, ProfileService } from '@/services/api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ChevronLeft, Send, Plus, Video, Info, Mic, Calendar, CheckCircle2, UserPlus, UserMinus } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import RatingModal from '@/components/RatingModal';
import { auth } from '@/lib/firebase';

const ChatPage = () => {
  const navigate = useNavigate();
  
  // ESTADOS DE DADOS REAIS
  const [conversations, setConversations] = useState<any[]>([]);
  const [selectedConvo, setSelectedConvo] = useState<any>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [message, setMessage] = useState('');
  const [targetProfile, setTargetProfile] = useState<any>(null);
  
  // ESTADOS DE UI
  const [showInfoPanel, setShowInfoPanel] = useState(false);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isRatingModalOpen, setIsRatingModalOpen] = useState(false);
  
  const socket = useRef<WebSocket | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const [myData, setMyData] = useState<any>({});

  useEffect(() => {
    UserService.getMe().then((response) => setMyData(response.data)).catch(() => undefined);
  }, []);

  // 1. CARREGAR LISTA DE CONVERSAS DO BACKEND
  useEffect(() => {
    ChatService.getConversations()
      .then(res => {
        const data = Array.isArray(res.data) ? res.data : [];
        setConversations(data);
        if (data.length > 0) handleSelectConversation(data[0]);
      })
      .catch(() => {
          console.warn("Rota /conversations não encontrada ou vazia no servidor.");
          setConversations([]);
      });
      
    return () => socket.current?.close();
  }, []);

  // 2. SELECIONAR CONVERSA E SINCRONIZAR HISTÓRICO
  const handleSelectConversation = async (convo: any) => {
    setSelectedConvo(convo);
    setShowInfoPanel(false);
    
    // Busca histórico real (Java: /history/{id1}/{id2})
    if (myData.userId && convo.user?.id) {
        ChatService.getHistory(myData.userId, convo.user.id)
          .then(res => {
            setMessages(res.data || []);
            scrollToBottom();
          })
          .catch(() => toast.error("Erro ao carregar histórico do banco."));
    }

    // Busca dados do perfil lateral
    if (convo.user?.username) {
        ProfileService.get(convo.user.username)
            .then(res => {
                setTargetProfile(res.data);
                setIsFollowing(res.data.isFollowedByMe || false); 
            })
            .catch(() => console.log("Perfil detalhado não disponível."));
    }

    // Gerenciamento de WebSocket
    if (socket.current) socket.current.close();
    if (auth.currentUser) {
      const wsUrl = BASE_URL.replace(/^http/, 'ws').replace(/\/api\/?$/, '/chat');
      socket.current = new WebSocket(wsUrl);
      socket.current.onopen = async () => {
        const token = await auth.currentUser?.getIdToken();
        if (token && socket.current?.readyState === WebSocket.OPEN) {
          socket.current.send(JSON.stringify({ type: 'auth', token }));
        }
      };
      socket.current.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          setMessages(prev => [...prev, msg]);
          scrollToBottom();
        } catch (e) { console.error("Erro na decodificação WS."); }
      };
    }
  };

  const scrollToBottom = () => {
    setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  };

  // 3. ENVIO DE MENSAGEM COM PERSISTÊNCIA
  const handleSendMessage = () => {
    if (!message.trim() || !selectedConvo) return;
    
    const payload = { 
      content: message, 
      senderId: myData.userId,
      receiverId: selectedConvo.user.id,
      type: 'TEXT'
    };
    
    if (socket.current?.readyState === WebSocket.OPEN) {
        socket.current.send(JSON.stringify(payload));
        setMessages(prev => [...prev, { ...payload, isOwn: true, timestamp: new Date().toISOString() }]);
        setMessage('');
        scrollToBottom();
    } else {
        // Fallback HTTP real para o ChatController.java
        ChatService.sendMessage(payload)
            .then(res => {
                setMessages(prev => [...prev, res.data]);
                setMessage('');
                scrollToBottom();
            }).catch(() => toast.error("Conexão com servidor de chat falhou."));
    }
  };

  const toggleFollow = async () => {
      if (!targetProfile) return;
      try {
          if (isFollowing) {
              await UserService.unfollowUser(targetProfile.id);
              setIsFollowing(false);
              toast.success("Deixou de seguir.");
          } else {
              await UserService.followUser(targetProfile.id);
              setIsFollowing(true);
              toast.success("Seguindo!");
          }
      } catch (e) { toast.error("Falha na operação de rede."); }
  };

  const submitRating = async (data: { proposalId: string; stars: number; comment: string }) => {
      try {
          await RatingService.submit({ 
              proposalId: Number(data.proposalId) || selectedConvo?.proposalId || 0, 
              stars: data.stars, 
              comment: data.comment 
          });
          toast.success("Avaliação salva no banco de dados!");
          setIsRatingModalOpen(false);
      } catch (error) { toast.error("Erro ao salvar avaliação."); }
  };

  const formatMessageTime = (dateString: string) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="max-w-7xl mx-auto p-4 h-[calc(100vh-80px)] flex flex-col font-sans">
      <Link to="/" className="inline-flex items-center gap-2 text-foreground font-bold mb-4 hover:text-skillswap-teal transition-colors">
        <ChevronLeft size={20} /> Voltar ao Feed
      </Link>

      <div className="flex flex-1 gap-4 bg-card border border-border rounded-3xl overflow-hidden shadow-2xl relative">
        {/* LISTAGEM DE CONVERSAS */}
        <div className={`w-80 border-r border-border bg-muted/10 flex flex-col transition-all ${selectedConvo ? 'hidden md:flex' : 'flex w-full'}`}>
          <div className="p-6 border-b border-border bg-background/50 font-bold text-xl">💬 Conversas</div>
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {conversations.map((convo) => (
              <div
                key={convo?.id}
                onClick={() => handleSelectConversation(convo)}
                className={`p-3 rounded-2xl cursor-pointer transition-all ${selectedConvo?.id === convo?.id ? 'bg-skillswap-teal text-white shadow-md' : 'hover:bg-muted'}`}
              >
                <div className="flex items-center gap-3">
                  <Avatar className="h-12 w-12 border-2 border-background shrink-0">
                    <AvatarImage src={convo?.user?.avatarUrl} />
                    <AvatarFallback className="bg-muted text-foreground font-bold">{convo?.user?.username?.[0]?.toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold truncate">{convo?.user?.username}</p>
                    <p className={`text-xs truncate ${selectedConvo?.id === convo?.id ? 'text-teal-50' : 'text-muted-foreground'}`}>
                      {convo?.lastMessage || "Nova conversa..."}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ÁREA CENTRAL DO CHAT */}
        {selectedConvo ? (
          <div className={`flex-1 flex flex-col bg-background/30 transition-all ${showInfoPanel ? 'hidden lg:flex' : 'flex'}`}>
            <div className="p-4 border-b border-border flex items-center justify-between bg-background/80 backdrop-blur-md z-10">
              <div className="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity" onClick={() => navigate(`/perfil/${selectedConvo?.user?.username}`)}>
                <Avatar className="h-10 w-10 border border-border"><AvatarImage src={selectedConvo?.user?.avatarUrl} /></Avatar>
                <div className="min-w-0">
                    <span className="font-bold text-base block truncate">{selectedConvo?.user?.name || selectedConvo?.user?.username}</span>
                    <p className="text-xs text-muted-foreground">@{selectedConvo?.user?.username}</p>
                </div>
              </div>
              <div className="flex gap-2 shrink-0">
                 <Button variant="default" size="sm" className="hidden sm:flex bg-green-600 hover:bg-green-700 text-white font-bold" onClick={() => setIsRatingModalOpen(true)}>
                    <CheckCircle2 size={16} className="mr-2"/> Encerrar Curso
                 </Button>
                 <Button variant="ghost" size="icon" onClick={() => setShowInfoPanel(!showInfoPanel)}>
                    <Info size={20} className={showInfoPanel ? "text-skillswap-teal" : ""} />
                 </Button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
              {messages.map((msg, i) => (
                <div key={i} className={`flex flex-col ${msg?.senderId === myData.userId || msg?.isOwn ? 'items-end' : 'items-start'}`}>
                  <div className={`max-w-[75%] px-4 py-2 text-sm shadow-sm relative ${msg?.senderId === myData.userId || msg?.isOwn ? 'bg-skillswap-teal text-white rounded-2xl rounded-tr-none' : 'bg-card border border-border rounded-2xl rounded-tl-none'}`}>
                    <p className="whitespace-pre-wrap break-words">{msg?.content}</p>
                  </div>
                  <span className="text-[10px] text-muted-foreground mt-1 px-1">{formatMessageTime(msg.timestamp || msg.sentAt)}</span>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {/* BARRA DE INPUT */}
            <div className="p-4 bg-background border-t border-border">
              <div className="flex items-end gap-2 bg-muted/30 p-2 rounded-3xl border border-border focus-within:ring-1 focus-within:ring-skillswap-teal transition-shadow">
                <Button variant="ghost" size="icon" className="rounded-full shrink-0" onClick={() => fileInputRef.current?.click()}><Plus size={22} /></Button>
                <input type="file" ref={fileInputRef} hidden />
                <Textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSendMessage(); } }}
                  placeholder="Escreva sua mensagem..."
                  className="flex-1 bg-transparent border-none shadow-none resize-none focus-visible:ring-0 min-h-[40px] max-h-[120px] py-3 text-sm"
                  rows={1}
                />
                <Button onClick={handleSendMessage} className="bg-skillswap-teal hover:bg-teal-700 rounded-full w-10 h-10 shrink-0 shadow-md">
                    <Send size={18} />
                </Button>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center text-muted-foreground flex-col gap-4 text-center p-8">
             <Send className="text-skillswap-teal w-12 h-12 opacity-20 ml-2" />
             <p className="font-bold text-lg text-foreground">Sua Central de Trocas</p>
             <p className="text-sm max-w-xs">Selecione um navegador na lista para iniciar uma troca de conhecimento.</p>
          </div>
        )}

        {/* PAINEL DE DETALHES LATERAL DIREITO */}
        {selectedConvo && showInfoPanel && (
           <div className="w-72 border-l border-border bg-card flex flex-col animate-in slide-in-from-right-8">
              <div className="p-6 flex flex-col items-center border-b border-border text-center">
                 <Avatar className="h-24 w-24 mb-4 border-2 border-skillswap-teal"><AvatarImage src={targetProfile?.avatarUrl || selectedConvo.user?.avatarUrl} /></Avatar>
                 <h3 className="font-bold text-lg truncate w-full px-2">{targetProfile?.name || selectedConvo.user?.username}</h3>
                 <p className="text-sm text-muted-foreground mb-4">@{selectedConvo.user?.username}</p>
                 <div className="flex flex-col gap-2 w-full px-4">
                    <Button variant={isFollowing ? "outline" : "default"} className={`w-full ${!isFollowing ? 'bg-skillswap-teal text-white' : ''}`} onClick={toggleFollow}>
                        {isFollowing ? <><UserMinus size={16} className="mr-2"/> Deixar</> : <><UserPlus size={16} className="mr-2"/> Seguir</>}
                    </Button>
                    <Button variant="outline" className="w-full" onClick={() => navigate(`/perfil/${selectedConvo.user.username}`)}>Ver Perfil</Button>
                 </div>
              </div>
           </div>
        )}
      </div>
      
      {/* MODAL DE AVALIAÇÃO - INTEGRADO COM BACKEND */}
      {selectedConvo && (
        <RatingModal 
            open={isRatingModalOpen} 
            onOpenChange={setIsRatingModalOpen} 
            onSubmit={submitRating} 
            user={selectedConvo.user}
            proposalId={selectedConvo.proposalId || "0"}
        />
      )}
    </div>
  );
};

export default ChatPage;
