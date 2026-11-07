import { useEffect, useState, useRef } from 'react';
import { ChatService, BASE_URL, RatingService, UserService, ProfileService, LessonService } from '@/services/api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ChevronLeft, Send, Plus, Video, Info, Mic, Calendar, CheckCircle2, UserPlus, UserMinus } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import RatingModal from '@/components/RatingModal';
import { auth } from '@/lib/firebase';

const ChatPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const openChatWith = (location.state as any)?.openChatWith;
  const openChatTargetId = openChatWith?.userId || openChatWith?.id;
  
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
  const [isRecording, setIsRecording] = useState(false);
  const [showSchedule, setShowSchedule] = useState(false);
  const [scheduledAt, setScheduledAt] = useState('');
  
  const socket = useRef<WebSocket | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  const [myData, setMyData] = useState<any>({});

  const getConversationUserId = (convo: any) => convo?.user?.id || convo?.user?.userId;

  const appendUniqueMessage = (incoming: any) => {
    setMessages((current) => {
      const incomingId = incoming?.messageId ?? incoming?.id;
      if (incomingId !== undefined && current.some((item) => String(item?.messageId ?? item?.id) === String(incomingId))) {
        return current;
      }
      return [...current, incoming];
    });
  };

  useEffect(() => {
    UserService.getMe().then((response) => setMyData(response.data)).catch(() => undefined);
  }, []);

  // 1. CARREGAR LISTA DE CONVERSAS DO BACKEND
  useEffect(() => {
    // Espera o usuário autenticado estar carregado para que a conversa direta
    // aberta a partir de um perfil consiga buscar o histórico correto.
    if (!myData.userId) return;

    ChatService.getConversations()
      .then(res => {
        const data = Array.isArray(res.data) ? res.data : [];
        const existingConversation = openChatTargetId
          ? data.find((convo: any) => String(convo?.user?.id || convo?.user?.userId) === String(openChatTargetId))
          : undefined;
        const keepConversationListOpen = (conversationData: any[]) => {
          // A lista é a tela inicial do chat, inclusive no celular. A conversa
          // só abre depois que a pessoa escolhe um contato.
          setConversations(conversationData);
          if (openChatWith) navigate(location.pathname, { replace: true, state: {} });
        };

        if (openChatWith && openChatTargetId && !existingConversation) {
          // Não criamos uma conversa apenas no navegador. O backend só libera
          // o chat depois que uma proposta entre os dois usuários for aceita.
          ChatService.canMessage(String(openChatTargetId)).then((permission) => {
            if (!permission.data?.allowed) {
              toast.info('Envie uma proposta e aguarde a aceitação para iniciar a conversa.');
              keepConversationListOpen(data);
              return;
            }
            const directConversation = {
              id: `direct-${openChatTargetId}`,
              user: {
                id: openChatTargetId,
                userId: openChatTargetId,
                username: openChatWith.username,
                name: openChatWith.name,
                avatarUrl: openChatWith.avatarUrl || openChatWith.profile?.imageUrl,
              },
              lastMessage: 'Conversa liberada',
            };
            keepConversationListOpen([directConversation, ...data]);
          }).catch(() => {
            toast.info('Envie uma proposta e aguarde a aceitação para iniciar a conversa.');
            keepConversationListOpen(data);
          });
        } else {
          keepConversationListOpen(data);
        }
      })
      .catch(() => {
          console.warn("Rota /conversations não encontrada ou vazia no servidor.");
          setConversations([]);
      });
      
    return () => socket.current?.close();
  }, [myData.userId, openChatTargetId, navigate, location.pathname]);

  // Mantém a lista atualizada para que conversas e mensagens novas apareçam
  // sem abrir automaticamente a primeira conversa.
  useEffect(() => {
    if (!myData.userId) return;
    const refreshConversations = () => {
      ChatService.getConversations()
        .then((res) => setConversations(Array.isArray(res.data) ? res.data : []))
        .catch(() => undefined);
    };
    const interval = window.setInterval(refreshConversations, 7000);
    return () => window.clearInterval(interval);
  }, [myData.userId]);

  // 2. SELECIONAR CONVERSA E SINCRONIZAR HISTÓRICO
  const handleSelectConversation = async (convo: any) => {
    setSelectedConvo(convo);
    setShowInfoPanel(false);
    
    // Busca histórico real (Java: /history/{id1}/{id2})
    const conversationUserId = getConversationUserId(convo);
    if (myData.userId && conversationUserId) {
        ChatService.getHistory(myData.userId, conversationUserId)
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
          if (msg?.type === 'error') {
            toast.error(msg.message || 'Não foi possível enviar a mensagem.');
            return;
          }
          const currentContactId = String(conversationUserId);
          const senderId = String(msg?.senderId || msg?.sender?.userId || msg?.sender?.id || '');
          const receiverId = String(msg?.receiverId || msg?.receiver?.userId || msg?.receiver?.id || '');
          const myUserId = String(myData.userId);
          const belongsToConversation =
            (senderId === myUserId && receiverId === currentContactId) ||
            (senderId === currentContactId && receiverId === myUserId);
          if (!belongsToConversation) return;
          appendUniqueMessage(msg);
          scrollToBottom();
        } catch (e) { console.error("Erro na decodificação WS."); }
      };
    }
  };

  useEffect(() => {
    const conversationUserId = getConversationUserId(selectedConvo);
    if (!myData.userId || !conversationUserId) return;
    const refreshHistory = () => {
      ChatService.getHistory(myData.userId, conversationUserId)
        .then((res) => setMessages(Array.isArray(res.data) ? res.data : []))
        .catch(() => undefined);
    };
    const interval = window.setInterval(refreshHistory, 5000);
    return () => window.clearInterval(interval);
  }, [myData.userId, selectedConvo?.id, selectedConvo?.user?.id, selectedConvo?.user?.userId]);

  const scrollToBottom = () => {
    setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  };

  // 3. ENVIO DE MENSAGEM COM PERSISTÊNCIA
  const handleSendMessage = async () => {
    if (!message.trim() || !selectedConvo) return;
    
    const payload = { 
      content: message, 
      senderId: myData.userId,
      receiverId: selectedConvo.user.id || selectedConvo.user.userId,
      type: 'TEXT'
    };
    
    try {
      // HTTP é a confirmação oficial de persistência. O backend publica a
      // mesma mensagem no WebSocket depois de salvar no banco.
      const { data } = await ChatService.sendMessage(payload);
      appendUniqueMessage(data);
      setMessage('');
      scrollToBottom();
    } catch {
      // O WebSocket continua como fallback quando a rota HTTP estiver
      // temporariamente indisponível.
      if (socket.current?.readyState === WebSocket.OPEN) {
        socket.current.send(JSON.stringify(payload));
        setMessage('');
        toast.info('Mensagem enviada pela conexão em tempo real.');
      } else {
        toast.error("Conexão com servidor de chat falhou.");
      }
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    const receiverId = getConversationUserId(selectedConvo);
    if (!file || !receiverId) return;
    if (file.size > 20 * 1024 * 1024) {
      toast.error('O arquivo deve ter no máximo 20 MB.');
      event.target.value = '';
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    formData.append('receiverId', receiverId);
    try {
      const { data } = await ChatService.uploadAttachment(formData);
      appendUniqueMessage(data);
      toast.success('Arquivo enviado.');
    } catch { toast.error('Não foi possível enviar o arquivo.'); }
    event.target.value = '';
  };

  const startAudioRecording = async () => {
    if (isRecording || !selectedConvo) return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const supportedMimeType = [
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/ogg;codecs=opus',
        'audio/mp4',
      ].find((mimeType) => MediaRecorder.isTypeSupported(mimeType));
      const recorder = supportedMimeType
        ? new MediaRecorder(stream, { mimeType: supportedMimeType })
        : new MediaRecorder(stream);
      audioChunksRef.current = [];
      recorder.ondataavailable = (event) => { if (event.data.size > 0) audioChunksRef.current.push(event.data); };
      recorder.onstop = async () => {
        stream.getTracks().forEach((track) => track.stop());
        const receiverId = getConversationUserId(selectedConvo);
        if (!receiverId) return;
        const formData = new FormData();
        const voiceType = recorder.mimeType || 'audio/webm';
        const extension = voiceType.includes('ogg') ? 'ogg' : voiceType.includes('mp4') ? 'm4a' : 'webm';
        formData.append('voice', new Blob(audioChunksRef.current, { type: voiceType }), `mensagem.${extension}`);
        formData.append('receiverId', receiverId);
        try {
          const { data } = await ChatService.sendVoiceMessage(formData);
          appendUniqueMessage(data);
        } catch { toast.error('Não foi possível enviar o áudio.'); }
      };
      mediaRecorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
    } catch { toast.error('Permita o microfone para gravar uma mensagem de áudio.'); }
  };

  const stopAudioRecording = () => {
    mediaRecorderRef.current?.stop();
    mediaRecorderRef.current = null;
    setIsRecording(false);
  };

  const scheduleLesson = async () => {
    const teacherId = selectedConvo?.user?.id || selectedConvo?.user?.userId;
    if (!teacherId || !myData.userId || !scheduledAt) return;
    try {
      await LessonService.schedule({
        teacherId,
        studentId: myData.userId,
        scheduledTime: new Date(scheduledAt).toISOString(),
      });
      setShowSchedule(false);
      setScheduledAt('');
      toast.success('Aula agendada. Os participantes foram notificados.');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Não foi possível agendar a aula.');
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

  const openSharedCall = () => {
      const peerId = selectedConvo?.user?.id || selectedConvo?.user?.userId;
      if (!peerId || !myData.userId) return;
      const room = `skillswap-call-${[String(myData.userId), String(peerId)].sort().join('-')}`;
      const peer = selectedConvo?.user || {};
      const params = new URLSearchParams({
          room,
          peerName: peer.name || peer.username || 'Participante',
          peerUsername: peer.username || '',
          peerAvatar: peer.profile?.imageUrl || peer.avatarUrl || peer.imageUrl || '',
      });
      navigate(`/chamada/${peerId}?${params.toString()}`);
  };

  return (
    <div className="max-w-7xl mx-auto w-full p-3 pb-24 sm:p-4 sm:pb-6 min-h-[calc(100dvh-4rem)] flex flex-col font-sans">
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
          <div className={`min-h-0 flex-1 flex flex-col bg-background/30 transition-all ${showInfoPanel ? 'hidden lg:flex' : 'flex'}`}>
            <div className="p-4 border-b border-border flex items-center justify-between bg-background/80 backdrop-blur-md z-10">
              <div className="flex min-w-0 items-center gap-2 sm:gap-3">
                <Button variant="ghost" size="icon" className="md:hidden shrink-0" aria-label="Voltar para conversas" onClick={() => { socket.current?.close(); setSelectedConvo(null); setMessages([]); }}>
                  <ChevronLeft size={20} />
                </Button>
                <Avatar className="h-10 w-10 border border-border"><AvatarImage src={selectedConvo?.user?.avatarUrl} /></Avatar>
                <div className="min-w-0 cursor-pointer hover:opacity-80 transition-opacity" onClick={() => navigate(`/perfil/${selectedConvo?.user?.username}`)}>
                    <span className="font-bold text-base block truncate">{selectedConvo?.user?.name || selectedConvo?.user?.username}</span>
                    <p className="text-xs text-muted-foreground">@{selectedConvo?.user?.username}</p>
                </div>
              </div>
              <div className="flex flex-wrap justify-end gap-1 sm:gap-2 shrink-0">
                 <Button variant="outline" size="sm" className="h-9 w-9 shrink-0 px-0 sm:h-10 sm:w-auto sm:px-3" title="Iniciar chamada de vídeo e áudio" onClick={openSharedCall} disabled={!selectedConvo?.user?.id && !selectedConvo?.user?.userId}>
                    <Video size={16} className="sm:mr-2" /><span className="hidden sm:inline">Chamada</span>
                 </Button>
                 <Button variant="outline" size="sm" className="h-9 w-9 px-0 sm:h-10 sm:w-auto sm:px-3" title="Agendar aula" onClick={() => setShowSchedule((current) => !current)}><Calendar size={16} className="sm:mr-2" /><span className="hidden sm:inline">Agendar</span></Button>
                 <Button variant="default" size="sm" className="h-9 w-9 px-0 sm:h-10 sm:w-auto sm:px-3 bg-green-600 hover:bg-green-700 text-white font-bold" title="Encerrar curso" onClick={() => setIsRatingModalOpen(true)}>
                    <CheckCircle2 size={16} className="sm:mr-2"/><span className="hidden sm:inline">Encerrar Curso</span>
                 </Button>
                 <Button variant="ghost" size="icon" onClick={() => setShowInfoPanel(!showInfoPanel)}>
                    <Info size={20} className={showInfoPanel ? "text-skillswap-teal" : ""} />
                 </Button>
              </div>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
              {messages.map((msg, i) => {
                const senderId = msg?.senderId || msg?.sender?.userId || msg?.sender?.id;
                const ownMessage = String(senderId) === String(myData.userId) || msg?.isOwn;
                return (
                <div key={msg?.messageId || msg?.id || i} className={`flex flex-col ${ownMessage ? 'items-end' : 'items-start'}`}>
                  <div className={`max-w-[90%] sm:max-w-[75%] px-4 py-2 text-sm shadow-sm relative ${ownMessage ? 'bg-skillswap-teal text-white rounded-2xl rounded-tr-none' : 'bg-card border border-border rounded-2xl rounded-tl-none'}`}>
                    {msg?.content && <p className="whitespace-pre-wrap break-words">{msg.content}</p>}
                    {msg?.voiceData && <audio controls className="max-w-full" src={`data:${msg.voiceType || 'audio/webm'};base64,${msg.voiceData}`} />}
                    {msg?.fileData && String(msg.fileType || '').startsWith('image/') && (
                      <img className="max-h-72 max-w-full rounded-lg object-contain" alt="Imagem enviada na conversa" src={`data:${msg.fileType};base64,${msg.fileData}`} />
                    )}
                    {msg?.fileData && String(msg.fileType || '').startsWith('video/') && (
                      <video controls className="max-h-72 max-w-full rounded-lg" src={`data:${msg.fileType};base64,${msg.fileData}`} />
                    )}
                    {msg?.fileData && !String(msg.fileType || '').startsWith('image/') && !String(msg.fileType || '').startsWith('video/') && (
                      <a className="underline" download="arquivo" href={`data:${msg.fileType || 'application/octet-stream'};base64,${msg.fileData}`}>Baixar arquivo</a>
                    )}
                  </div>
                  <span className="text-[10px] text-muted-foreground mt-1 px-1">{formatMessageTime(msg.timestamp || msg.sentAt)}</span>
                </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {showSchedule && <div className="border-t border-border bg-card p-3">
              <div className="flex flex-wrap items-end gap-2">
                <label className="text-xs font-semibold text-muted-foreground">Data e hora da aula
                  <input type="datetime-local" min={new Date().toISOString().slice(0, 16)} value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} className="mt-1 block rounded-md border border-border bg-background px-2 py-2 text-sm" />
                </label>
                <Button type="button" onClick={() => void scheduleLesson()} disabled={!scheduledAt} className="bg-skillswap-teal text-white">Salvar na Agenda</Button>
                <Link to="/agenda"><Button type="button" variant="outline">Abrir Agenda</Button></Link>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">A aula fica registrada na Agenda SkillSwap. A integração com o Google Calendar é opcional.</p>
            </div>}

            {/* BARRA DE INPUT */}
            <div className="p-4 bg-background border-t border-border">
              <div className="flex items-end gap-2 bg-muted/30 p-2 rounded-3xl border border-border focus-within:ring-1 focus-within:ring-skillswap-teal transition-shadow">
                <Button variant="ghost" size="icon" className="rounded-full shrink-0" onClick={() => fileInputRef.current?.click()}><Plus size={22} /></Button>
                <input type="file" accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.xls,.xlsx,.txt" ref={fileInputRef} hidden onChange={(event) => void handleFileUpload(event)} />
                <Button variant={isRecording ? 'destructive' : 'ghost'} size="icon" className="rounded-full shrink-0" onClick={() => isRecording ? stopAudioRecording() : void startAudioRecording()} aria-label={isRecording ? 'Parar gravação' : 'Gravar mensagem de áudio'}><Mic size={20} /></Button>
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
             <p className="text-sm max-w-xs">Selecione uma conversa na lista para iniciar uma troca de conhecimento.</p>
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
