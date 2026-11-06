import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { Maximize2, Mic, MicOff, MonitorUp, PhoneOff, Video, VideoOff } from 'lucide-react';
import { Room, RoomEvent, Track } from 'livekit-client';
import { Button } from '@/components/ui/button';
import { VideoService } from '@/services/api';
import { toast } from 'sonner';

const livekitUrl = (import.meta.env.VITE_LIVEKIT_URL as string | undefined)
  || 'wss://skillswap-video-lxnf4r1k.livekit.cloud';

const VideoCallPage = () => {
  const { userId = '' } = useParams();
  const [searchParams] = useSearchParams();
  const roomName = searchParams.get('room') || `skillswap-${[userId, 'shared'].sort().join('-')}`;
  const mediaContainer = useRef<HTMLDivElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const roomRef = useRef<Room | null>(null);
  const [connected, setConnected] = useState(false);
  const [cameraEnabled, setCameraEnabled] = useState(false);
  const [microphoneEnabled, setMicrophoneEnabled] = useState(false);
  const [screenEnabled, setScreenEnabled] = useState(false);

  useEffect(() => {
    let disposed = false;
    const room = new Room({ adaptiveStream: true, dynacast: true });
    roomRef.current = room;

    const attachTrack = (track: any) => {
      if (!mediaContainer.current || !track) return;
      if (track.sid && mediaContainer.current.querySelector(`[data-track-sid="${track.sid}"]`)) return;
      const element = track.attach();
      element.dataset.trackSid = track.sid || '';
      element.autoplay = true;
      element.playsInline = true;
      element.className = track.kind === Track.Kind.Video
        ? 'h-full min-h-48 w-full rounded-xl bg-black object-contain aspect-video'
        : 'hidden';
      mediaContainer.current.appendChild(element);
    };
    const detachTrack = (track: any) => {
      track?.detach().forEach((element: HTMLElement) => element.remove());
    };

    room.on(RoomEvent.TrackSubscribed, (track) => attachTrack(track));
    room.on(RoomEvent.TrackUnsubscribed, (track) => detachTrack(track));
    room.on(RoomEvent.LocalTrackPublished, (publication) => {
      if (publication.track) attachTrack(publication.track);
    });
    room.on(RoomEvent.Disconnected, () => setConnected(false));

    const connect = async () => {
      try {
        const { data } = await VideoService.getJoinToken(roomName, userId);
        await room.connect(livekitUrl, data.token);
        if (disposed) return;
        setConnected(true);
        await room.localParticipant.enableCameraAndMicrophone();
        setCameraEnabled(true);
        setMicrophoneEnabled(true);
        room.localParticipant.trackPublications.forEach((publication) => {
          if (publication.track) attachTrack(publication.track);
        });
      } catch (error: any) {
        toast.error(error?.response?.data?.error || 'Não foi possível entrar na chamada.');
      }
    };
    void connect();

    return () => {
      disposed = true;
      room.disconnect();
      roomRef.current = null;
      if (mediaContainer.current) mediaContainer.current.replaceChildren();
    };
  }, [roomName]);

  const toggleCamera = async () => {
    if (!roomRef.current) return;
    const next = !cameraEnabled;
    try {
      await roomRef.current.localParticipant.setCameraEnabled(next);
      setCameraEnabled(next);
    } catch {
      toast.error('Não foi possível acessar a câmera. Verifique a permissão do navegador.');
    }
  };

  const toggleMicrophone = async () => {
    if (!roomRef.current) return;
    const next = !microphoneEnabled;
    try {
      await roomRef.current.localParticipant.setMicrophoneEnabled(next);
      setMicrophoneEnabled(next);
    } catch {
      toast.error('Não foi possível acessar o microfone. Verifique a permissão do navegador.');
    }
  };

  const toggleScreen = async () => {
    if (!roomRef.current) return;
    const next = !screenEnabled;
    try {
      await roomRef.current.localParticipant.setScreenShareEnabled(next);
      setScreenEnabled(next);
    } catch {
      toast.error('Não foi possível compartilhar a tela. O navegador pode ter bloqueado a captura.');
    }
  };

  const toggleFullscreen = async () => {
    try {
      if (document.fullscreenElement) await document.exitFullscreen();
      else await stageRef.current?.requestFullscreen();
    } catch {
      toast.error('Não foi possível abrir a chamada em tela cheia.');
    }
  };

  return (
    <main className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl flex-col gap-4 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-xl font-bold">Sala SkillSwap</h1><p className="text-sm text-muted-foreground">{connected ? 'Conectado' : 'Conectando...'} · {roomName}</p></div>
        <Link to="/mensagens"><Button variant="outline"><PhoneOff className="mr-2 h-4 w-4" />Sair da chamada</Button></Link>
      </div>
      <div ref={stageRef} className="relative min-h-[50vh] rounded-2xl bg-slate-950 p-3">
        <div ref={mediaContainer} className="grid h-full min-h-[50vh] auto-rows-fr gap-3 sm:grid-cols-2" aria-live="polite" />
        <Button type="button" variant="secondary" size="icon" className="absolute right-5 top-5" onClick={() => void toggleFullscreen()} aria-label="Abrir chamada em tela cheia">
          <Maximize2 className="h-4 w-4" />
        </Button>
      </div>
      <div className="flex flex-wrap justify-center gap-3">
        <Button type="button" variant={microphoneEnabled ? 'default' : 'destructive'} onClick={() => void toggleMicrophone()}><>{microphoneEnabled ? <Mic className="mr-2 h-4 w-4" /> : <MicOff className="mr-2 h-4 w-4" />}</>{microphoneEnabled ? 'Microfone' : 'Ativar microfone'}</Button>
        <Button type="button" variant={cameraEnabled ? 'default' : 'destructive'} onClick={() => void toggleCamera()}><>{cameraEnabled ? <Video className="mr-2 h-4 w-4" /> : <VideoOff className="mr-2 h-4 w-4" />}</>{cameraEnabled ? 'Câmera' : 'Ativar câmera'}</Button>
        <Button type="button" variant={screenEnabled ? 'default' : 'outline'} onClick={() => void toggleScreen()}><MonitorUp className="mr-2 h-4 w-4" />{screenEnabled ? 'Parar tela' : 'Compartilhar tela'}</Button>
      </div>
    </main>
  );
};

export default VideoCallPage;
