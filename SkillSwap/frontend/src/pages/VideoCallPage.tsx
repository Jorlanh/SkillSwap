import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { Maximize2, Mic, MicOff, MonitorUp, PhoneOff, Video, VideoOff } from 'lucide-react';
import { Room, RoomEvent, Track } from 'livekit-client';
import { Button } from '@/components/ui/button';
import { UserService, VideoService } from '@/services/api';
import { toast } from 'sonner';

const livekitUrl = (import.meta.env.VITE_LIVEKIT_URL as string | undefined)
  || 'wss://skillswap-video-lxnf4r1k.livekit.cloud';

const buildRoomName = (first: string, second: string) =>
  `skillswap-call-${[first, second].map(String).sort().join('-')}`;

const VideoCallPage = () => {
  const { userId = '' } = useParams();
  const [searchParams] = useSearchParams();
  const requestedRoom = searchParams.get('room');
  const peerName = searchParams.get('peerName') || 'Participante';
  const peerUsername = searchParams.get('peerUsername') || '';
  const peerAvatar = searchParams.get('peerAvatar') || '';
  const mediaContainer = useRef<HTMLDivElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const roomRef = useRef<Room | null>(null);
  const [roomName, setRoomName] = useState(requestedRoom || '');
  const [connected, setConnected] = useState(false);
  const [remoteCount, setRemoteCount] = useState(0);
  const [hasVideo, setHasVideo] = useState(false);
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
      setHasVideo(elements.length > 0);

      elements.forEach((element, index) => {
        const isScreen = element === screen;
        if (screen) {
          element.className = isScreen
            ? 'absolute inset-0 h-full w-full rounded-xl bg-black object-contain'
            : 'absolute bottom-3 right-3 z-20 h-28 w-44 rounded-xl border-2 border-white/80 bg-black object-cover shadow-2xl sm:bottom-4 sm:right-4 sm:h-40 sm:w-64';
          return;
        }

        if (elements.length > 1) {
          element.className = `absolute inset-y-0 ${index === 0 ? 'left-0' : 'right-0'} h-full w-1/2 rounded-xl bg-black object-contain`;
        } else {
          element.className = 'absolute inset-0 h-full w-full rounded-xl bg-black object-contain';
        }
      });
    };

    const attachTrack = (track: any, isLocal = false) => {
      const container = mediaContainer.current;
      if (!container || !track) return;

      if (track.kind === Track.Kind.Audio) {
        if (isLocal) return;
        if (track.sid && container.querySelector(`[data-track-sid="${track.sid}"]`)) return;
        const audio = track.attach() as HTMLAudioElement;
        audio.dataset.trackSid = track.sid || '';
        audio.dataset.audioTrack = 'true';
        audio.autoplay = true;
        audio.controls = false;
        audio.setAttribute('aria-label', `Áudio de ${peerName}`);
        container.appendChild(audio);
        return;
      }

      if (track.kind !== Track.Kind.Video) return;
      if (track.sid && container.querySelector(`[data-track-sid="${track.sid}"]`)) return;

      const video = track.attach() as HTMLVideoElement;
      video.dataset.trackSid = track.sid || '';
      video.dataset.videoTrack = 'true';
      video.dataset.trackSource = String(track.source || '');
      video.autoplay = true;
      video.playsInline = true;
      video.muted = isLocal;
      video.setAttribute('aria-label', track.source === Track.Source.ScreenShare
        ? `Tela compartilhada por ${peerName}`
        : `Câmera de ${isLocal ? 'você' : peerName}`);
      container.appendChild(video);
      void video.play().catch(() => undefined);
      layoutTracks();
    };

    const detachTrack = (track: any) => {
      track?.detach().forEach((element: Element) => element.remove());
      layoutTracks();
    };

    const syncRemoteParticipants = () => {
      setRemoteCount(room.remoteParticipants.size);
      room.remoteParticipants.forEach((participant) => {
        participant.trackPublications.forEach((publication) => {
          if (publication.isSubscribed && publication.track) attachTrack(publication.track, false);
        });
      });
    };

    room.on(RoomEvent.TrackSubscribed, (track) => attachTrack(track, false));
    room.on(RoomEvent.TrackUnsubscribed, (track) => detachTrack(track));
    room.on(RoomEvent.ParticipantConnected, syncRemoteParticipants);
    room.on(RoomEvent.ParticipantDisconnected, syncRemoteParticipants);
    room.on(RoomEvent.LocalTrackPublished, (publication) => {
      if (publication.source === Track.Source.ScreenShare) setScreenEnabled(true);
      if (publication.track) attachTrack(publication.track, true);
    });
    room.on(RoomEvent.LocalTrackUnpublished, (publication) => {
      if (publication.source === Track.Source.ScreenShare) setScreenEnabled(false);
      if (publication.track) detachTrack(publication.track);
    });
    room.on(RoomEvent.Disconnected, () => {
      setConnected(false);
      setRemoteCount(0);
      setHasVideo(false);
    });

    const connect = async () => {
      try {
        const { data: profile } = await UserService.getMe();
        const currentUserId = String(profile?.id || profile?.userId || '');
        const resolvedRoom = requestedRoom || (currentUserId && userId
          ? buildRoomName(currentUserId, userId)
          : `skillswap-call-${userId}-shared`);

        setRoomName(resolvedRoom);
        const { data } = await VideoService.getJoinToken(resolvedRoom, userId);
        await room.connect(livekitUrl, data.token);
        if (disposed) return;
        setConnected(true);
        syncRemoteParticipants();

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
          if (publication.track) attachTrack(publication.track, true);
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
  }, [requestedRoom, userId, peerName]);

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

  const initial = peerName.trim().charAt(0).toUpperCase() || '?';

  return (
    <main className="mx-auto flex min-h-[calc(100dvh-4rem)] max-w-7xl flex-col gap-3 p-2 sm:gap-4 sm:p-4">
      <div className="flex flex-wrap items-center justify-between gap-2 sm:gap-3">
        <div className="min-w-0">
          <h1 className="truncate text-lg font-bold sm:text-xl">Sala SkillSwap</h1>
          <p className="max-w-[75vw] truncate text-xs text-muted-foreground sm:max-w-none sm:text-sm">
            {connected ? 'Conectado' : 'Conectando...'} · {roomName || 'preparando sala'}
          </p>
        </div>
        <Link to="/mensagens" className="shrink-0">
          <Button variant="outline" size="sm"><PhoneOff className="mr-2 h-4 w-4" />Sair da chamada</Button>
        </Link>
      </div>

      <div ref={stageRef} data-call-stage className="relative h-[calc(100dvh-13rem)] min-h-[24rem] overflow-hidden rounded-2xl bg-slate-950 p-1.5 sm:h-[min(78vh,720px)] sm:min-h-[32rem] sm:p-3">
        <div ref={mediaContainer} className="relative h-full min-h-full w-full overflow-hidden rounded-xl" aria-live="polite" />

        {!hasVideo && (
          <div className="pointer-events-none absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 p-6 text-center text-white">
            {peerAvatar ? (
              <img src={peerAvatar} alt="" className="h-20 w-20 rounded-full border-2 border-white/70 object-cover" />
            ) : (
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-skillswap-teal text-3xl font-bold">{initial}</div>
            )}
            <div>
              <p className="font-semibold">{remoteCount > 0 ? peerName : 'Aguardando a outra pessoa entrar'}</p>
              {peerUsername && <p className="text-sm text-white/70">@{peerUsername.replace(/^@/, '')}</p>}
              <p className="mt-1 text-xs text-white/60">
                {remoteCount > 0 ? 'A pessoa está na sala. Câmera e tela podem estar desligadas.' : 'A chamada usa uma sala compartilhada entre os dois participantes.'}
              </p>
            </div>
          </div>
        )}

        {remoteCount > 0 && (
          <div className="absolute left-3 top-3 z-30 flex items-center gap-2 rounded-full bg-black/60 px-3 py-1.5 text-xs text-white backdrop-blur sm:left-5 sm:top-5">
            {peerAvatar && <img src={peerAvatar} alt="" className="h-5 w-5 rounded-full object-cover" />}
            <span>{peerName}{peerUsername ? ` · @${peerUsername.replace(/^@/, '')}` : ''}</span>
          </div>
        )}

        <Button type="button" variant="secondary" size="icon" className="absolute right-3 top-3 z-30 sm:right-5 sm:top-5" onClick={() => void toggleFullscreen()} aria-label="Abrir chamada em tela cheia">
          <Maximize2 className="h-4 w-4" />
        </Button>
      </div>

      <div className="flex flex-wrap justify-center gap-2 sm:gap-3">
        <Button type="button" size="sm" variant={microphoneEnabled ? 'default' : 'destructive'} onClick={() => void toggleMicrophone()}>
          {microphoneEnabled ? <Mic className="mr-2 h-4 w-4" /> : <MicOff className="mr-2 h-4 w-4" />}
          {microphoneEnabled ? 'Microfone' : 'Ativar microfone'}
        </Button>
        <Button type="button" size="sm" variant={cameraEnabled ? 'default' : 'destructive'} onClick={() => void toggleCamera()}>
          {cameraEnabled ? <Video className="mr-2 h-4 w-4" /> : <VideoOff className="mr-2 h-4 w-4" />}
          {cameraEnabled ? 'Câmera' : 'Ativar câmera'}
        </Button>
        <Button type="button" size="sm" variant={screenEnabled ? 'default' : 'outline'} onClick={() => void toggleScreen()}>
          <MonitorUp className="mr-2 h-4 w-4" />{screenEnabled ? 'Parar tela' : 'Compartilhar tela'}
        </Button>
      </div>
    </main>
  );
};

export default VideoCallPage;
