import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { UserService, AchievementService, PostService, ProfileService, ProposalService } from '@/services/api';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { MapPin, Calendar, Mail, Award, BookOpen, Layers, Users, Edit3, Camera, Lock, Unlock, Send, Star, GraduationCap, Image as ImageIcon, Trash2, Plus, Eye, Upload } from 'lucide-react';
import SkillBadge from '@/components/SkillBadge';
import PostCard from '@/components/PostCard';
import RightSidebar from '@/components/RightSidebar';
import VerifiedBadge from '@/components/VerifiedBadge';
import VisualMarker from '@/components/VisualMarker';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent } from '@/components/ui/card';
import { toast } from 'sonner';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

const tabs = ['Publicações', 'Cursos', 'Conquistas', 'Atividade', 'Comunidades'];

const ProfilePage = () => {
  const { username: urlUsername } = useParams();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('Publicações');
  const [user, setUser] = useState<any>(null);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [achievements, setAchievements] = useState<any[]>([]);
  const [userPosts, setUserPosts] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // Modais
  const [isEditing, setIsEditing] = useState(false);
  const [isCoursesModalOpen, setIsCoursesModalOpen] = useState(false); 

  // Estados Dinâmicos de Interação
  const [isFollowing, setIsFollowing] = useState(false);

  // Estados de Edição
  const [editName, setEditName] = useState("");
  const [editUsername, setEditUsername] = useState("");
  const [editBio, setEditBio] = useState("");
  const [editLocation, setEditLocation] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [skillsList, setSkillsList] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState("");
  
  // Postagem
  const [newPostContent, setNewPostContent] = useState("");
  const [postImage, setPostImage] = useState<File | null>(null);

  // Proposta
  const [isProposalOpen, setIsProposalOpen] = useState(false);
  const [proposalMessage, setProposalMessage] = useState("");

  // Foto Menu
  const [isViewPhotoOpen, setIsViewPhotoOpen] = useState(false);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);

  const ratings = {
    teacher: user?.ratings?.teacher || 0.0,
    student: user?.ratings?.student || 0.0,
    totalReviews: user?.ratings?.totalReviews || 0
  };

  const loadProfileData = async () => {
    setIsLoading(true);
    try {
      const { data: meData } = await UserService.getMe();
      let targetUser;

      if (!urlUsername || urlUsername === meData.username) {
        targetUser = meData;
        setIsOwnProfile(true);
        if (!urlUsername) navigate(`/perfil/${meData.username}`, { replace: true });
      } else {
        setIsOwnProfile(false);
        const { data: publicProfile } = await ProfileService.get(urlUsername);
        targetUser = publicProfile;
      }

      setUser(targetUser);
      
      setEditName(targetUser.name || "");
      setEditUsername(targetUser.username || "");
      setEditBio(targetUser.profile?.description || targetUser.bio || "");
      setEditLocation(targetUser.profile?.location || targetUser.location || "");
      setIsPrivate(targetUser.profile?.isPrivate || false);
      setSkillsList(targetUser.skills?.map((s: any) => s.name) || []);
      
      // Verifica Followers
      if (meData.following && Array.isArray(meData.following)) {
        setIsFollowing(meData.following.some((f: any) => f.id === targetUser.id));
      }

      const tId = targetUser.userId || targetUser.id;
      if (tId) {
        const [achRes, postsRes] = await Promise.allSettled([
          AchievementService.getUserAchievements(tId),
          PostService.getAll() 
        ]);

        if (achRes.status === 'fulfilled') setAchievements(achRes.value.data || []);
        if (postsRes.status === 'fulfilled') {
          const allPosts = Array.isArray(postsRes.value.data) ? postsRes.value.data : [];
          setUserPosts(allPosts.filter((p: any) => (p.user?.userId === tId || p.userId === tId)));
        }
      }
    } catch (error) {
      toast.error("Erro ao carregar perfil do banco.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadProfileData(); }, [urlUsername]);

  const handleSaveProfile = async () => {
    try {
      await UserService.updateProfile({ 
        name: editName,
        username: editUsername,
        description: editBio, 
        location: editLocation,
        skills: skillsList 
      });
      toast.success("Perfil atualizado!");
      setIsEditing(false);
      setIsCoursesModalOpen(false); 
      
      if (editUsername !== user.username && isOwnProfile) {
        navigate(`/perfil/${editUsername}`, { replace: true });
        setTimeout(() => { window.location.reload(); }, 500);
      } else {
        loadProfileData();
      }
    } catch (error) {
      toast.error("Erro ao salvar. Verifique se o username já existe.");
    }
  };

  const handleRemoveSkill = async (skillToRemove: string) => {
    try {
       const newSkills = skillsList.filter(s => s !== skillToRemove);
       setSkillsList(newSkills); 
       
       await UserService.updateProfile({ 
        name: editName,
        username: editUsername,
        description: editBio, 
        location: editLocation,
        skills: newSkills 
      });
      
      toast.success("Curso removido com sucesso!");
      loadProfileData(); 
    } catch (error) {
      toast.error("Erro ao tentar remover o curso.");
    }
  };

  const handleAvatarUpload = async (file: File) => {
    try {
      const loading = toast.loading("Fazendo upload da foto...");
      await UserService.updateAvatar(file);
      toast.dismiss(loading);
      toast.success("Foto do perfil atualizada!");
      loadProfileData();
    } catch (error) {
      toast.error("Erro ao enviar imagem.");
    }
  };

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) {
      handleAvatarUpload(e.target.files[0]);
    }
  };

  const handleRemoveAvatar = async () => {
     try {
         toast.info("Removendo foto...");
         toast.warning("Rota de remoção de avatar ainda pendente no backend.");
     } catch (error) {
         toast.error("Erro ao remover foto.");
     }
  };

  const handleCreatePost = async () => {
    if (!newPostContent.trim()) return;
    try {
      const formData = new FormData();
      formData.append('content', newPostContent);
      
      if (postImage !== null) {
          formData.append('image', postImage);
      }
      
      await PostService.create(formData);
      
      setNewPostContent("");
      setPostImage(null);
      toast.success("Postagem realizada com sucesso!");
      loadProfileData();
    } catch (error) {
      toast.error("Erro ao publicar post. Verifique a conexão com /api/posts.");
    }
  };

  const handleSendProposal = async () => {
    if (!proposalMessage.trim()) return;
    try {
      const targetId = user.userId || user.id;
      // DTO Atualizado para prevenir Erros 400 no Backend
      await ProposalService.create({
        receiverId: targetId,
        message: proposalMessage,
        skillOfferedId: null, 
        skillWantedId: null
      } as any);
      
      toast.success("Proposta enviada! Acompanhe em Solicitações.");
      setIsProposalOpen(false);
      setProposalMessage("");
      navigate('/propostas');
    } catch (error) {
      toast.error("Erro ao enviar proposta para a API.");
    }
  };

  const handleToggleFollow = async () => {
    const targetId = user.userId || user.id;
    try {
      if (isFollowing) {
        await UserService.unfollowUser(targetId);
        toast.info(`Você deixou de seguir ${user.username}.`);
        setIsFollowing(false);
      } else {
        await UserService.followUser(targetId);
        toast.success(`Você começou a seguir ${user.username}!`);
        setIsFollowing(true);
      }
      loadProfileData();
    } catch (error) {
      toast.error("Erro de conexão ao tentar seguir usuário.");
    }
  };

  // NAVEGAÇÃO PARA O CHAT
  const handleOpenChat = () => {
    // Redireciona e passa o targetUser no state para que a página de chat já abra a conversa dele
    navigate('/mensagens', { state: { openChatWith: user } });
  };

  const handleAddSkill = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && skillInput.trim()) {
      e.preventDefault();
      if (!skillsList.includes(skillInput.trim())) {
        setSkillsList([...skillsList, skillInput.trim()]);
      }
      setSkillInput("");
    }
  };

  const togglePrivacy = async () => {
    const newStatus = !isPrivate;
    try {
      await UserService.togglePrivacy(newStatus);
      setIsPrivate(newStatus);
      toast.success(newStatus ? "Perfil Privado" : "Perfil Público");
    } catch (error) {
      toast.error("Erro ao alterar privacidade.");
    }
  };

  const openCamera = async () => {
    setIsCameraOpen(true);
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({ video: true });
      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
    } catch (err) {
      toast.error("Permissão de câmera negada ou câmera não encontrada.");
      setIsCameraOpen(false);
    }
  };

  const closeCamera = useCallback(() => {
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      setStream(null);
    }
    setIsCameraOpen(false);
  }, [stream]);

  const capturePhoto = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth;
      canvas.height = videoRef.current.videoHeight;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => {
          if (blob) {
            const file = new File([blob], "webcam_photo.jpg", { type: "image/jpeg" });
            handleAvatarUpload(file);
            closeCamera();
          }
        }, 'image/jpeg');
      }
    }
  };

  if (isLoading) return <ProfileSkeleton />;

  return (
    <div className="flex gap-6 max-w-[1200px] mx-auto p-4">
      {/* CORREÇÃO VISUAL DA LARGURA: Expansão do max-w para dar espaço ao Card de Reputação */}
      <div className="flex-1 min-w-0">
        
        {/* CABEÇALHO */}
        <div className="flex flex-col md:flex-row items-start gap-6 mb-6 bg-card p-6 rounded-2xl border border-border shadow-sm">
          
          <div className="relative group shrink-0">
            <Avatar className="h-28 w-28 border-4 border-skillswap-teal ring-4 ring-skillswap-teal/10">
              <AvatarImage src={user?.profile?.imageUrl || user?.avatarUrl} />
              <AvatarFallback className="text-3xl font-bold bg-muted">{user?.username?.[0]?.toUpperCase()}</AvatarFallback>
            </Avatar>

            {isOwnProfile && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="absolute inset-0 flex items-center justify-center bg-black/60 rounded-full opacity-0 group-hover:opacity-100 transition-opacity">
                    <Camera className="text-white h-8 w-8" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-56 bg-card border-border shadow-2xl p-2 rounded-xl">
                  <DropdownMenuItem className="cursor-pointer font-semibold py-2" onClick={() => setIsViewPhotoOpen(true)}>
                    <Eye className="mr-2 h-4 w-4" /> Mostrar foto
                  </DropdownMenuItem>
                  <DropdownMenuItem className="cursor-pointer font-semibold py-2" onClick={openCamera}>
                    <Camera className="mr-2 h-4 w-4" /> Tirar foto
                  </DropdownMenuItem>
                  <DropdownMenuItem className="cursor-pointer font-semibold py-2" onClick={() => fileInputRef.current?.click()}>
                    <Upload className="mr-2 h-4 w-4" /> Carregar foto
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem className="cursor-pointer font-semibold py-2 text-red-500 focus:text-red-500" onClick={handleRemoveAvatar}>
                    <Trash2 className="mr-2 h-4 w-4" /> Remover foto
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
            <input type="file" ref={fileInputRef} hidden accept="image/*" onChange={handleAvatarChange} />
          </div>

          <div className="flex-1 w-full min-w-0">
            <div className="flex justify-between items-start gap-3 mb-2 flex-wrap">
              <div>
                <h1 className="font-display font-bold text-2xl text-foreground flex items-center gap-2 truncate">
                  {user?.name || user?.username} <VerifiedBadge user={user} />
                  <VisualMarker marker={user?.accessibilityVisualMarker} />
                </h1>
                <p className="text-skillswap-teal font-semibold">@{user?.username?.toLowerCase()}</p>
              </div>
              
              <div className="flex gap-2 shrink-0">
                {!isOwnProfile ? (
                  <>
                    <Button onClick={() => setIsProposalOpen(true)} className="bg-skillswap-teal hover:bg-teal-700 text-white font-bold rounded-full px-6">
                      Enviar Proposta
                    </Button>
                    <Button onClick={handleToggleFollow} variant="outline" size="sm" className="rounded-full font-bold px-4 border-skillswap-teal text-skillswap-teal hover:bg-skillswap-teal hover:text-white transition-colors">
                       {isFollowing ? "Deixar de Seguir" : "Seguir"}
                    </Button>
                    {/* BOTÃO CHAT CORRIGIDO */}
                    <Button variant="ghost" size="icon" className="h-8 w-8 rounded-full bg-muted hover:bg-skillswap-teal hover:text-white" onClick={handleOpenChat}>
                       <Mail size={16} />
                    </Button>
                  </>
                ) : (
                  <Button variant="outline" size="sm" onClick={() => setIsEditing(true)} className="rounded-full font-bold px-4 flex gap-2 border-skillswap-teal text-skillswap-teal">
                    <Edit3 size={14} /> Editar Perfil
                  </Button>
                )}
              </div>
            </div>
            
            <p className="text-sm text-foreground mb-4 leading-relaxed max-w-lg break-words italic">
              {user?.profile?.description || user?.bio || "Nenhuma biografia definida no banco."}
            </p>
            
            <div className="flex flex-wrap items-center gap-4 text-[10px] font-bold text-muted-foreground mb-4 uppercase">
              <span className="flex items-center gap-1 bg-muted px-2 py-1 rounded-md"><MapPin size={12} /> {user?.profile?.location || user?.location || "Sem localização"}</span>
              <span className="flex items-center gap-1 bg-muted px-2 py-1 rounded-md">
                <Calendar size={12} /> Entrou em {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '--/--/----'}
              </span>
            </div>

            <div className="flex gap-6 pt-4 border-t border-border/50">
              <div className="text-center"><p className="font-bold text-lg">{user?.followers || 0}</p><p className="text-[10px] uppercase text-muted-foreground font-bold">Seguidores</p></div>
              <div className="text-center"><p className="font-bold text-lg">{user?.following || 0}</p><p className="text-[10px] uppercase text-muted-foreground font-bold">Seguindo</p></div>
              <div className="text-center"><p className="font-bold text-lg text-skillswap-teal">{user?.swapCoins || 0}</p><p className="text-[10px] uppercase text-muted-foreground font-bold">SwapCoins</p></div>
            </div>
          </div>

          {/* COLUNA DIREITA: CSS DO CARD DE REPUTAÇÃO CORRIGIDO PARA W-72 (LARGO) E FLEX-WRAP */}
          <div className="hidden md:block w-72 shrink-0 space-y-6">
            <div className="bg-card border border-border p-5 rounded-xl shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-[10px] font-bold text-muted-foreground uppercase tracking-tighter">Reputação Real</h3>
                <span className="text-[10px] font-bold text-foreground">({ratings.totalReviews} avaliações)</span>
              </div>
              <div className="space-y-4">
                <div className="flex justify-between items-center text-xs font-bold bg-muted/30 px-3 py-2 rounded-lg">
                  <span className="flex items-center gap-2"><GraduationCap size={16} className="text-skillswap-teal"/> Professor</span>
                  <div className="flex items-center gap-1"><span className="text-sm">{ratings.teacher}</span><Star size={14} className="fill-yellow-400 text-yellow-400" /></div>
                </div>
                <div className="flex justify-between items-center text-xs font-bold bg-muted/30 px-3 py-2 rounded-lg">
                  <span className="flex items-center gap-2"><BookOpen size={16} className="text-skillswap-teal"/> Aluno</span>
                  <div className="flex items-center gap-1"><span className="text-sm">{ratings.student}</span><Star size={14} className="fill-yellow-400 text-yellow-400" /></div>
                </div>
              </div>
            </div>

            <div>
              <h3 className="text-[10px] font-bold text-muted-foreground uppercase mb-3 flex items-center gap-1"><Award size={12}/> Habilidades</h3>
              <div className="flex flex-wrap gap-2">
                {user?.skills?.length > 0 ? user.skills.map((s: any) => (
                  <SkillBadge key={s.id || Math.random()} skill={s.name} />
                )) : <span className="text-[10px] text-muted-foreground">Nenhuma definida.</span>}
              </div>
            </div>
          </div>
        </div>

        {/* NAVEGAÇÃO DE TABS */}
        <div className="flex border-b border-border mb-6 overflow-x-auto no-scrollbar">
          {tabs.map((tab) => (
            <button key={tab} className={`px-6 py-4 text-sm font-bold transition-all border-b-2 whitespace-nowrap ${activeTab === tab ? 'text-skillswap-teal border-skillswap-teal' : 'text-muted-foreground border-transparent hover:text-foreground'}`} onClick={() => setActiveTab(tab)}>{tab}</button>
          ))}
        </div>

        {/* CONTEÚDO DINÂMICO */}
        <div className="min-h-[400px]">
          {activeTab === 'Publicações' && (
            <div className="space-y-4">
              {isOwnProfile && (
                <div className="bg-card rounded-2xl border border-border p-4 shadow-sm mb-6">
                  <div className="flex gap-3">
                    <Avatar className="h-10 w-10 shrink-0"><AvatarImage src={user?.avatarUrl} /></Avatar>
                    <div className="flex-1 space-y-3">
                      <Textarea value={newPostContent} onChange={(e) => setNewPostContent(e.target.value)} placeholder="O que você quer ensinar hoje?" className="resize-none border-transparent bg-muted/30 focus-visible:ring-1 min-h-[80px]" />
                      <div className="flex justify-between items-center">
                        <div className="flex gap-2">
                          <input type="file" id="post-img" hidden onChange={e => setPostImage(e.target.files?.[0] || null)} />
                          <label htmlFor="post-img" className="cursor-pointer p-2 hover:bg-muted rounded-full text-muted-foreground"><ImageIcon size={20}/></label>
                          {postImage && <span className="text-[10px] text-skillswap-teal mt-2">1 Imagem Selecionada</span>}
                        </div>
                        <Button onClick={handleCreatePost} disabled={!newPostContent.trim()} className="bg-skillswap-teal hover:bg-teal-700 text-white font-bold rounded-xl px-6">Publicar <Send className="ml-2 h-4 w-4" /></Button>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              {userPosts.map((post) => <PostCard key={post.id || Math.random()} post={post} />)}
            </div>
          )}

          {activeTab === 'Cursos' && (
            <div className="space-y-4">
              {isOwnProfile && (
                // CORREÇÃO: Abre especificamente o modal de Cursos
                <Button variant="outline" className="w-full border-dashed border-2 py-10 rounded-2xl flex flex-col gap-2 opacity-50 hover:opacity-100 transition-opacity" onClick={() => setIsCoursesModalOpen(true)}>
                  <Plus size={24}/> <span className="font-bold uppercase text-[10px]">Adicionar novo curso</span>
                </Button>
              )}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {user?.skills?.map((s: any) => (
                  <Card key={s.id || Math.random()} className="group relative border-border bg-card/50">
                    <CardContent className="p-4 flex justify-between items-center">
                      <div className="flex items-center gap-3">
                        <div className="p-2 bg-skillswap-teal/10 rounded-lg text-skillswap-teal"><BookOpen size={20}/></div>
                        <div><p className="font-bold text-sm">{s.name}</p><p className="text-[10px] text-muted-foreground font-bold uppercase">Disponível para troca</p></div>
                      </div>
                      {isOwnProfile && <button onClick={() => handleRemoveSkill(s.name)} className="text-red-500 opacity-0 group-hover:opacity-100 transition-opacity p-2 bg-red-100 rounded-full hover:bg-red-200"><Trash2 size={16}/></button>}
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'Conquistas' && (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              {achievements.length > 0 ? achievements.map((ach) => (
                <Card key={ach.id || Math.random()} className="overflow-hidden group hover:border-skillswap-teal transition-all bg-card/50 border-border">
                  <CardContent className="p-6 flex flex-col items-center text-center">
                    <div className="w-16 h-16 rounded-full bg-skillswap-teal/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                      {ach.imageUrl ? (
                        <img src={ach.imageUrl} alt={ach.achievement?.name} className="w-10 h-10 object-contain" />
                      ) : (
                        <Award size={32} className="text-skillswap-teal" />
                      )}
                    </div>
                    <span className="text-sm font-bold text-foreground">{ach.achievement?.name || "Conquista"}</span>
                    <span className="text-[10px] text-muted-foreground mt-1 line-clamp-2">{ach.achievement?.description}</span>
                  </CardContent>
                </Card>
              )) : (
                <div className="col-span-full"><EmptyState icon={<Award />} message="Nenhuma conquista desbloqueada." /></div>
              )}
            </div>
          )}
        </div>

        {/* MODAIS DE CÂMERA E FOTO */}
        <Dialog open={isViewPhotoOpen} onOpenChange={setIsViewPhotoOpen}>
          <DialogContent className="max-w-lg bg-transparent border-none shadow-none flex justify-center items-center">
             <DialogHeader className="hidden"><DialogTitle>Foto de Perfil</DialogTitle><DialogDescription>Foto em alta resolução</DialogDescription></DialogHeader>
             <img src={user?.profile?.imageUrl || user?.avatarUrl} alt="Foto de Perfil" className="w-full h-auto rounded-full border-4 border-skillswap-teal max-w-[400px] object-cover" />
          </DialogContent>
        </Dialog>

        <Dialog open={isCameraOpen} onOpenChange={(open) => { if (!open) closeCamera(); else setIsCameraOpen(true); }}>
          <DialogContent className="max-w-md bg-card">
            <DialogHeader>
              <DialogTitle>Tirar Foto</DialogTitle>
              <DialogDescription className="hidden">Câmera ativa.</DialogDescription>
            </DialogHeader>
            <div className="flex flex-col items-center gap-4">
              <div className="relative w-full aspect-square bg-black rounded-full overflow-hidden flex items-center justify-center border-4 border-muted">
                <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover transform scale-x-[-1]" />
              </div>
              <Button onClick={capturePhoto} className="bg-skillswap-teal text-white rounded-full h-16 w-16 shadow-lg flex items-center justify-center p-0">
                <Camera size={28} />
              </Button>
            </div>
          </DialogContent>
        </Dialog>

        {/* MODAL DE EDIÇÃO DO PERFIL GERAL */}
        <Dialog open={isEditing} onOpenChange={setIsEditing}>
          <DialogContent className="max-w-md bg-card border-border shadow-2xl">
            <DialogHeader>
               <DialogTitle className="font-bold text-xl text-skillswap-teal">Configurações de Perfil</DialogTitle>
               <DialogDescription className="hidden">Painel de edição.</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1"><label className="text-[10px] font-bold uppercase text-muted-foreground">Nome Exibido</label><Input value={editName} onChange={e => setEditName(e.target.value)} /></div>
                <div className="space-y-1"><label className="text-[10px] font-bold uppercase text-muted-foreground">Username Único</label><Input value={editUsername} onChange={e => setEditUsername(e.target.value)} /></div>
              </div>
              <div className="space-y-1"><label className="text-[10px] font-bold uppercase text-muted-foreground">Biografia</label><Textarea value={editBio} onChange={e => setEditBio(e.target.value)} className="resize-none" rows={3}/></div>
              <div className="space-y-1"><label className="text-[10px] font-bold uppercase text-muted-foreground">Localização</label><Input value={editLocation} onChange={e => setEditLocation(e.target.value)} /></div>
              <div className="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                <div className="flex items-center gap-2"><Lock size={16}/> <span className="text-xs font-bold uppercase">Perfil Privado</span></div>
                <Button variant={isPrivate ? "default" : "outline"} size="sm" onClick={togglePrivacy} className={isPrivate ? "bg-skillswap-teal" : ""}>{isPrivate ? "Ativado" : "Desativado"}</Button>
              </div>
            </div>
            <Button onClick={handleSaveProfile} className="w-full bg-skillswap-teal text-white font-bold h-12 shadow-lg">Confirmar no Banco</Button>
          </DialogContent>
        </Dialog>

        {/* MODAL ESPECÍFICO DE CURSOS */}
        <Dialog open={isCoursesModalOpen} onOpenChange={setIsCoursesModalOpen}>
          <DialogContent className="max-w-md bg-card border-border shadow-2xl">
            <DialogHeader>
               <DialogTitle className="font-bold text-xl text-skillswap-teal">Adicionar Cursos</DialogTitle>
               <DialogDescription className="hidden">Gerenciador de cursos.</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold uppercase text-muted-foreground">Novos Cursos (Pressione Enter para inserir)</label>
                <Input placeholder="Ex: Aulas de Xadrez, Culinária Italiana..." value={skillInput} onChange={e => setSkillInput(e.target.value)} onKeyDown={handleAddSkill} />
                <div className="flex flex-wrap gap-2 mt-4">
                  {skillsList.map(s => (
                    <span key={s} className="bg-skillswap-teal/10 text-skillswap-teal px-3 py-1.5 rounded-full text-xs font-bold flex items-center gap-2 group">
                      {s} <button onClick={() => setSkillsList(skillsList.filter(i => i !== s))} className="hover:text-red-500"><Trash2 size={12}/></button>
                    </span>
                  ))}
                </div>
              </div>
            </div>
            <Button onClick={handleSaveProfile} className="w-full bg-skillswap-teal text-white font-bold h-12 shadow-lg">Salvar Cursos</Button>
          </DialogContent>
        </Dialog>

        {/* MODAL DE PROPOSTA */}
        <Dialog open={isProposalOpen} onOpenChange={setIsProposalOpen}>
          <DialogContent className="bg-card border-border shadow-2xl">
            <DialogHeader>
                <DialogTitle className="font-bold text-xl">Enviar Proposta para {user?.username}</DialogTitle>
                <DialogDescription className="hidden">Proposta de skill.</DialogDescription>
            </DialogHeader>
            <div className="py-4 space-y-4">
              <Textarea placeholder="Descreva o que quer aprender e o que pode ensinar..." value={proposalMessage} onChange={e => setProposalMessage(e.target.value)} className="min-h-[150px] bg-muted/30 border-none" />
              <p className="text-[10px] text-muted-foreground italic font-bold">A proposta será enviada para o usuário e aparecerá na aba de Solicitações do destinatário.</p>
            </div>
            <DialogFooter><Button onClick={handleSendProposal} disabled={!proposalMessage.trim()} className="bg-skillswap-teal hover:bg-teal-700 w-full font-bold h-12">Enviar Proposta de Troca</Button></DialogFooter>
          </DialogContent>
        </Dialog>

      </div>
      <div className="hidden lg:block w-80"><RightSidebar /></div>
    </div>
  );
};

const EmptyState = ({ icon, message }: any) => (
  <div className="flex flex-col items-center justify-center py-20 text-muted-foreground bg-card/50 rounded-2xl border border-border w-full">
    <div className="mb-4 opacity-30 scale-150">{icon}</div>
    <p className="text-sm font-bold uppercase">{message}</p>
  </div>
);

const ProfileSkeleton = () => (
  <div className="max-w-5xl mx-auto p-4 space-y-6 flex gap-6">
    <div className="flex-1"><Skeleton className="h-64 w-full rounded-2xl" /><Skeleton className="h-screen w-full rounded-2xl mt-6" /></div>
    <div className="hidden lg:block w-80"><Skeleton className="h-screen w-full rounded-3xl" /></div>
  </div>
);

export default ProfilePage;
