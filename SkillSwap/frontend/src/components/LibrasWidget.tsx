import { useEffect } from 'react';

/** Official VLibras widget; it is loaded only in the browser and never blocks the app. */
export const LibrasWidget = () => {
  useEffect(() => {
    if (import.meta.env.VITE_ENABLE_VLIBRAS === 'false') return;
    const scriptId = 'skillswap-vlibras-script';
    if (!document.getElementById(scriptId)) {
      const script = document.createElement('script');
      script.id = scriptId;
      script.src = 'https://vlibras.gov.br/app/vlibras-plugin.js';
      script.async = true;
      script.onload = () => {
        const Widget = (window as any).VLibras?.Widget;
        if (Widget) new Widget('https://vlibras.gov.br/app');
      };
      document.body.appendChild(script);
    }
  }, []);

  return (
    <div {...({ vw: '', className: 'enabled' } as any)} aria-label="Tradutor de Libras">
      <div {...({ 'vw-access-button': '', className: 'active' } as any)}></div>
      <div {...({ 'vw-plugin-wrapper': '' } as any)}><div className="vw-plugin-top-wrapper"></div></div>
    </div>
  );
};
