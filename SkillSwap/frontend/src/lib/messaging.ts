import { getMessaging, getToken, isSupported, onMessage, type Messaging } from "firebase/messaging";
import { app } from "./firebase";
import { toast } from "sonner";
import { UserService } from "@/services/api";

let messaging: Messaging | null = null;

const getSupportedMessaging = async () => {
  if (!(await isSupported())) return null;
  if (!messaging) messaging = getMessaging(app);
  return messaging;
};

export const requestNotificationPermission = async () => {
  try {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) return;

    const config = {
      apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
      authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
      projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
      storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
      messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
      appId: import.meta.env.VITE_FIREBASE_APP_ID,
    };
    const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;
    if (Object.values(config).some((value) => !value || String(value).startsWith('your-')) || !vapidKey || vapidKey.startsWith('your-')) {
      return;
    }

    const supportedMessaging = await getSupportedMessaging();
    if (!supportedMessaging) return;

    const permission = await Notification.requestPermission();
    if (permission === "granted") {
      // Confere se o Railway está entregando o worker como JavaScript. Caso
      // a rota ainda esteja devolvendo o index.html, não tenta registrar um
      // arquivo inválido nem transforma isso em erro de login.
      const workerResponse = await fetch('/firebase-messaging-sw.js', { cache: 'no-store' });
      const contentType = workerResponse.headers.get('content-type') || '';
      if (!workerResponse.ok || (!contentType.includes('javascript') && !contentType.includes('text/plain'))) {
        console.warn('Notificações não ativadas: firebase-messaging-sw.js não foi publicado como JavaScript.');
        return;
      }
      const workerParams = new URLSearchParams(
        Object.entries(config).map(([key, value]) => [key, value || ''])
      );
      const serviceWorkerRegistration = await navigator.serviceWorker.register(
        `/firebase-messaging-sw.js?${workerParams.toString()}`,
        { scope: '/firebase-cloud-messaging-push-scope' }
      );

      const token = await getToken(supportedMessaging, {
        vapidKey,
        serviceWorkerRegistration,
      });
      
      if (token) {
        await UserService.updateFcmToken(token);
        return token;
      }
    } else {
      console.warn("Permissão de notificação negada pelo usuário.");
    }
    } catch (error) {
    console.warn("Notificações não foram ativadas neste navegador:", error);
  }
};

// Listener para quando o usuário está com o site aberto (ForeGround)
export const onMessageListener = () =>
  getSupportedMessaging().then((supportedMessaging) => {
    if (!supportedMessaging) return null;
    return new Promise((resolve) => {
      onMessage(supportedMessaging, (payload) => {
        // Exibe um toast bonitão na tela usando a Sonner
        toast.info(`${payload.notification?.title}: ${payload.notification?.body}`, {
          description: "Você tem uma nova atualização no SkillSwap.",
          duration: 5000,
        });
        resolve(payload);
      });
    });
  });
