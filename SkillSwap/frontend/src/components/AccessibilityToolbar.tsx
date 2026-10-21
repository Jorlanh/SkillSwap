import { useEffect, useState } from 'react';
import { Contrast, StopCircle, Volume2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

export const AccessibilityToolbar = () => {
  const [speaking, setSpeaking] = useState(false);

  const [highContrast, setHighContrast] = useState(() => localStorage.getItem('highContrast') === 'true');

  useEffect(() => {
    document.documentElement.classList.toggle('high-contrast', highContrast);
  }, [highContrast]);

  const toggleHighContrast = () => {
    const next = !highContrast;
    setHighContrast(next);
    localStorage.setItem('highContrast', String(next));
    document.documentElement.classList.toggle('high-contrast', next);
    window.dispatchEvent(new CustomEvent('skillswap:accessibility-changed'));
  };

  const readPage = () => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const text = document.querySelector('main')?.textContent?.replace(/\s+/g, ' ').trim();
    if (!text) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'pt-BR';
    utterance.rate = 0.92;
    utterance.onend = () => setSpeaking(false);
    setSpeaking(true);
    window.speechSynthesis.speak(utterance);
  };

  const stopReading = () => {
    window.speechSynthesis.cancel();
    setSpeaking(false);
  };

  return (
    <div className="fixed bottom-6 left-6 z-[90] flex gap-2" role="toolbar" aria-label="Ferramentas de leitura">
      <Button type="button" variant="outline" onClick={toggleHighContrast} aria-pressed={highContrast} aria-label={highContrast ? 'Desativar alto contraste' : 'Ativar alto contraste'} className="rounded-full shadow-lg bg-card">
        <Contrast size={17} />
        <span className="sr-only">{highContrast ? 'Desativar alto contraste' : 'Ativar alto contraste'}</span>
      </Button>
      <Button type="button" variant="outline" onClick={speaking ? stopReading : readPage} aria-label={speaking ? 'Parar leitura da página' : 'Ler página em voz alta'} className="rounded-full shadow-lg bg-card">
        {speaking ? <StopCircle size={17} /> : <Volume2 size={17} />}
        <span className="sr-only">{speaking ? 'Parar leitura' : 'Ler página'}</span>
      </Button>
    </div>
  );
};
