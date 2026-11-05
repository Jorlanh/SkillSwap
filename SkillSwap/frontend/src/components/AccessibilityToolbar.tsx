import { useState } from 'react';
import { StopCircle, Volume2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

export const AccessibilityToolbar = () => {
  const [speaking, setSpeaking] = useState(false);

  const readPage = () => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const text = document.querySelector('main')?.textContent?.replace(/\s+/g, ' ').trim();
    if (!text) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'pt-BR';
    utterance.rate = 0.92;
    utterance.pitch = 1.02;
    utterance.volume = 1;
    const voices = window.speechSynthesis.getVoices();
    utterance.voice = voices.find((voice) => voice.lang.toLowerCase() === 'pt-br')
      ?? voices.find((voice) => voice.lang.toLowerCase().startsWith('pt'))
      ?? null;
    utterance.onend = () => setSpeaking(false);
    setSpeaking(true);
    window.speechSynthesis.speak(utterance);
  };

  const stopReading = () => {
    window.speechSynthesis.cancel();
    setSpeaking(false);
  };

  return (
    <div className="mobile-floating-action fixed bottom-6 left-3 z-[90] flex gap-2 sm:left-6" role="toolbar" aria-label="Ferramentas de leitura">
      <Button type="button" variant="outline" onClick={speaking ? stopReading : readPage} aria-label={speaking ? 'Parar leitura da página' : 'Ler página em voz alta'} className="rounded-full shadow-lg bg-card">
        {speaking ? <StopCircle size={17} /> : <Volume2 size={17} />}
        <span className="sr-only">{speaking ? 'Parar leitura' : 'Ler página'}</span>
      </Button>
    </div>
  );
};
