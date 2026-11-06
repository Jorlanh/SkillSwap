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

    const layoutTracks = () => {
      const container = mediaContainer.current;
      if (!container) return;
      const elements = Array.from(container.querySelectorAll<HTMLElement>('[data-video-track="true"]'));
      const screen = elements.find((element) => element.dataset.trackSource === String(Track.Source.ScreenShare));
      elements.forEach((element) => {
        const isScreen = element === screen;
        element.className = screen
          ? (isScreen
            ? 'absolute inset-0 h-full w-full rounded-xl bg-black object-contain'
            : 'absolute bottom-4 right-4 z-20 h-32 w-52 rounded-xl border-2 border-white/80 bg-black object-cover shadow-2xl sm:h-40 sm:w-64')
          : 'absolute inset-0 h-full w-full rounded-xl bg-black object-contain';
      });
    };

    const attachTrack = (track: any, allowAudio = false) => {
      if (!mediaContainer.current || !track) return;
      if (track.kind === Track.Kind.Audio && allowAudio) {
        if (track.sid && mediaContainer.current.querySelector(`[data-track-sid="${track.sid}"]`)) return;
        const element = track.attach() as HTMLAudioElement;
        element.dataset.trackSid = track.sid || '';
        element.dataset.audioTrack = 'true';
        element.autoplay = true;
        element.controls = false;
        mediaContainer.current.appendChild(element);
        return;
      }
      if (track.kind !== Track.Kind.Video) return;
      if (track.sid && mediaContainer.current.querySelector(`[data-track-sid="${track.sid}"]`)) return;
      const element = track.attach() as HTMLVideoElement;
      element.dataset.trackSid = track.sid || '';
      element.dataset.videoTrack = 'true';
      element.dataset.trackSource = String(track.source || '');
      element.autoplay = true;
      element.playsInline = true;
      element.muted = track.source === Track.Source.Camera && !track.isSubscribed;
      mediaContainer.current.appendChild(element);
      layoutTracks();
    };

    const detachTrack = (track: any) => {
      track?.detach().forEach((element: HTMLElement) => element.remove());
      layoutTracks();
    };

    // TrackSubscribed é remoto: áudio e vídeo precisam ser anexados para que
    // o outro participante veja a câmera e ouça o microfone.
    room.on(RoomEvent.TrackSubscribed, (track) => attachTrack(track, true));
    room.on(RoomEvent.TrackUnsubscribed, (track) => detachTrack(track));
    room.on(RoomEvent.LocalTrackPublished, (publication) => {
      if (publication.track) attachTrack(publication.track);
    });
    room.on(RoomEvent.LocalTrackUnpublished, (publication) => {
      if (publication.track) detachTrack(publication.track);
    });
    room.on(RoomEvent.Disconnected, () => setConnected(false));

    const connect = async () => {
      try {
        const { data } = await VideoService.getJoinToken(roomName, userId);
        await room.connect(livekitUrl, data.token);
        if (disposed) return;
        setConnected(true);

        try {
          await room.localParticipant.setMicrophoneEnabled(true);
          setMicrophoneEnabled(true);
        } catch {
          toast.info('Microfone desativado. Você pode ativá-lo pelos controles da sala.');
        }
        try {
          await room.localParticipant.setCameraEnabled(true);
          setCameraEnabled(true);
        } catch {
          toast.info('Câmera desativada. Você pode ativá-la pelos controles da sala.');
        }
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
  }, [roomName, userId]);

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
    <main className="mx-auto flex min-h-[calc(100dvh-4rem)] max-w-7xl flex-col gap-4 p-3 sm:p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-xl font-bold">Sala SkillSwap</h1><p className="text-sm text-muted-foreground">{connected ? 'Conectado' : 'Conectando...'} · {roomName}</p></div>
        <Link to="/mensagens"><Button variant="outline"><PhoneOff className="mr-2 h-4 w-4" />Sair da chamada</Button></Link>
      </div>
      <div ref={stageRef} data-call-stage className="relative h-[min(78vh,720px)] min-h-[60vh] rounded-2xl bg-slate-950 p-2 sm:p-3">
        <div ref={mediaContainer} className="relative h-full min-h-full w-full overflow-hidden rounded-xl" aria-live="polite" />
        <Button type="button" variant="secondary" size="icon" className="absolute right-5 top-5 z-30" onClick={() => void toggleFullscreen()} aria-label="Abrir chamada em tela cheia">
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
