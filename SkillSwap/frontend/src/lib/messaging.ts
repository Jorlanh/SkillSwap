import { getMessaging, getToken, onMessage } from "firebase/messaging";
import { app } from "./firebase";
import { toast } from "sonner";
import { UserService } from "@/services/api";

const messaging = getMessaging(app);

export const requestNotificationPermission = async () => {
  try {
    const permission = await Notification.requestPermission();
    if (permission === "granted") {
      const token = await getToken(messaging, { 
        vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY 
      });
      
      if (token) {
        await UserService.updateFcmToken(token);
        return token;
      }
    } else {
      console.warn("Permissão de notificação negada pelo usuário.");
    }
  } catch (error) {
    console.error("Erro ao obter token de notificação:", error);
  }
};

// Listener para quando o usuário está com o site aberto (ForeGround)
export const onMessageListener = () =>
  new Promise((resolve) => {
    onMessage(messaging, (payload) => {
      // Exibe um toast bonitão na tela usando a Sonner
      toast.info(`${payload.notification?.title}: ${payload.notification?.body}`, {
        description: "Você tem uma nova atualização no SkillSwap.",
        duration: 5000,
      });
      resolve(payload);
    });
  });
