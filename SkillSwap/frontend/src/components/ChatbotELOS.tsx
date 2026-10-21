import { useState, type ChangeEvent, type KeyboardEvent } from 'react';
import { ChatbotService } from '@/services/api';
import { Bot, Send, BrainCircuit, X, MessageSquare, Volume2, Mic, MicOff } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Switch } from './ui/switch';
import { Label } from './ui/label';

interface ChatMessage { role: 'user' | 'bot'; text: string; }

const speak = (text: string) => {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'pt-BR';
  utterance.rate = 0.95;
  window.speechSynthesis.speak(utterance);
};

export const ChatbotELOS = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [simplify, setSimplify] = useState(false);
  const [voiceMode, setVoiceMode] = useState(false);
  const [listening, setListening] = useState(false);
  const [history, setHistory] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleSend = async () => {
    if (!message.trim()) return;
    setIsLoading(true);
    const userMsg = message.trim();
    setMessage('');
    setHistory((previous) => [...previous, { role: 'user', text: userMsg }]);
    try {
      const { data } = await ChatbotService.ask(userMsg, simplify);
      setHistory((previous) => [...previous, { role: 'bot', text: data.response }]);
      if (voiceMode) speak(data.response);
    } catch (error: any) {
      const fallback = error?.response?.status === 428
        ? 'Para usar o assistente, permita a integração de IA no Centro de Privacidade.'
        : 'Não foi possível conectar ao assistente. Tente novamente.';
      setHistory((previous) => [...previous, { role: 'bot', text: fallback }]);
      if (voiceMode) speak(fallback);
    } finally { setIsLoading(false); }
  };

  const startListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) { speak('O reconhecimento de voz não está disponível neste navegador.'); return; }
    const recognition = new SpeechRecognition();
    recognition.lang = 'pt-BR';
    recognition.interimResults = false;
    recognition.onstart = () => setListening(true);
    recognition.onend = () => setListening(false);
    recognition.onerror = () => setListening(false);
    recognition.onresult = (event: any) => setMessage(event.results[0][0].transcript);
    recognition.start();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => { if (event.key === 'Enter') handleSend(); };
  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => setMessage(event.target.value);

  return (
    <div className="fixed bottom-6 right-6 z-[100]" aria-label="Assistente de acessibilidade ELOS">
      {!isOpen ? (
        <Button onClick={() => setIsOpen(true)} aria-label="Abrir assistente ELOS" className="rounded-full w-14 h-14 shadow-lg bg-skillswap-teal focus-visible:ring-4 focus-visible:ring-teal-300">
          <MessageSquare className="h-6 w-6 text-white" />
        </Button>
      ) : (
        <div className="w-80 h-[480px] bg-card border border-border rounded-2xl shadow-2xl flex flex-col overflow-hidden" role="dialog" aria-modal="false" aria-labelledby="elos-title">
          <div className="bg-skillswap-teal p-4 flex justify-between items-center text-white">
            <div className="flex items-center gap-2"><Bot className="h-5 w-5" /><span id="elos-title" className="font-bold">MIND-ELOS</span></div>
            <Button variant="ghost" size="icon" aria-label="Fechar assistente" onClick={() => setIsOpen(false)} className="hover:bg-teal-700 text-white"><X className="h-4 w-4" /></Button>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-muted/30" role="log" aria-live="polite" aria-label="Conversa com o ELOS">
            {history.length === 0 && <p className="text-sm text-muted-foreground">Você pode digitar ou falar sua pergunta.</p>}
            {history.map((chat, index) => (
              <div key={index} className={`flex ${chat.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] p-3 rounded-2xl text-sm ${chat.role === 'user' ? 'bg-skillswap-teal text-white rounded-tr-none' : 'bg-card border text-foreground rounded-tl-none'}`}>
                  <span>{chat.text}</span>
                  {chat.role === 'bot' && <button type="button" aria-label="Ler resposta em voz alta" onClick={() => speak(chat.text)} className="ml-2 inline-flex align-middle text-skillswap-teal hover:text-teal-700"><Volume2 size={15} /></button>}
                </div>
              </div>
            ))}
          </div>

          <div className="p-4 border-t bg-card space-y-3">
            <div className="flex items-center justify-between gap-2">
              <Label htmlFor="mode-elos" className="text-[10px] uppercase font-bold text-muted-foreground flex items-center gap-1"><BrainCircuit className="h-3 w-3" /> Respostas simples</Label>
              <Switch id="mode-elos" checked={simplify} onCheckedChange={setSimplify} aria-label="Ativar respostas em linguagem simples" />
            </div>
            <div className="flex items-center justify-between gap-2">
              <Label htmlFor="voice-elos" className="text-[10px] uppercase font-bold text-muted-foreground flex items-center gap-1"><Volume2 className="h-3 w-3" /> Falar respostas</Label>
              <Switch id="voice-elos" checked={voiceMode} onCheckedChange={setVoiceMode} aria-label="Ler respostas em voz alta" />
            </div>
            <div className="flex gap-2">
              <Input value={message} onChange={handleInputChange} placeholder="Pergunte ao ELOS..." aria-label="Mensagem para o ELOS" onKeyDown={handleKeyDown} />
              <Button type="button" onClick={startListening} aria-label={listening ? 'Parar microfone' : 'Falar com o ELOS'} className="bg-muted text-foreground hover:bg-accent">{listening ? <MicOff className="h-4 w-4" /> : <Mic className="h-4 w-4" />}</Button>
              <Button type="button" onClick={handleSend} disabled={isLoading} aria-label="Enviar mensagem" className="bg-skillswap-teal"><Send className="h-4 w-4" /></Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
