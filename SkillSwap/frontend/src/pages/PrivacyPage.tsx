import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PrivacyService } from '@/services/api';

const PrivacyPage = () => {
  const [language, setLanguage] = useState<'pt' | 'en'>('pt');
  const [policy, setPolicy] = useState<Record<string, string> | null>(null);

  useEffect(() => {
    PrivacyService.getPolicy()
      .then(({ data }) => setPolicy(data))
      .catch(() => setPolicy(null));
  }, []);

  return (
    <main className="min-h-screen bg-background px-6 py-12 text-foreground">
      <article className="mx-auto max-w-4xl space-y-8 rounded-3xl border border-border bg-card p-8 shadow-sm">
        <header className="space-y-4">
          <p className="text-sm font-semibold uppercase tracking-wide text-skillswap-teal">SkillSwap</p>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h1 className="text-3xl font-bold">{language === 'pt' ? 'Política de Privacidade' : 'Privacy Policy'}</h1>
            <div className="flex gap-2" aria-label="Idioma da política">
              <button type="button" onClick={() => setLanguage('pt')} aria-pressed={language === 'pt'} className="rounded-lg border border-border px-3 py-2 text-sm">Português</button>
              <button type="button" onClick={() => setLanguage('en')} aria-pressed={language === 'en'} className="rounded-lg border border-border px-3 py-2 text-sm">English</button>
            </div>
          </div>
          <p className="text-sm text-muted-foreground">Version {policy?.version || '2026-10-21'} · {language === 'pt' ? 'documento de transparência do produto' : 'product transparency document'}</p>
        </header>

        {language === 'pt' ? <>
          <section className="space-y-3">
            <h2 className="text-xl font-semibold">1. Como usamos os dados</h2>
            <p>Usamos os dados necessários para autenticar a conta, manter o perfil, permitir a troca de habilidades, organizar aulas, proteger a comunidade e cumprir solicitações do titular.</p>
            <p>Não vendemos dados pessoais e não usamos a deficiência, o marcador visual ou a renda da pessoa para criar um diagnóstico. O marcador visual é uma preferência de interface e pode ser mantido privado.</p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold">2. Integrações opcionais</h2>
            <p>Vídeo, calendário, notificações, armazenamento de arquivos e assistentes de IA só devem ser habilitados quando forem necessários para a funcionalidade escolhida. O Centro de Privacidade permite revogar os consentimentos opcionais.</p>
            <p>Quando houver processamento fora do Brasil ou da União Europeia, informaremos o fornecedor, a finalidade, o país de destino, a duração e as garantias contratuais aplicáveis.</p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold">3. Direitos</h2>
            <p>A pessoa pode consultar, corrigir, exportar e solicitar a desativação da conta. A exportação reúne dados de conta, perfil, habilidades, consentimentos, registros de acesso, publicações, mensagens e aulas disponíveis no SkillSwap.</p>
            <p>Alguns registros podem ser mantidos de forma anonimizada quando forem necessários para segurança, prevenção de fraude, prestação de contas ou obrigação legal.</p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-semibold">4. Menores</h2>
            <p>Pessoas menores de 18 anos podem usar a plataforma. No cadastro, solicitamos nome, e-mail e vínculo de um responsável. O responsável recebe um link de uso único, e a conta continua acessível enquanto a confirmação estiver pendente.</p>
            <p>A confirmação por e-mail valida o controle do endereço informado, mas não comprova sozinha identidade ou autoridade legal. Antes da publicação em cada país, a equipe deve revisar exigências locais, especialmente quando houver usuários muito jovens.</p>
          </section>

        </> : <>
          <section className="space-y-3">
            <h2 className="text-xl font-semibold">1. How we use data</h2>
            <p>We use the data needed to authenticate accounts, maintain profiles, enable skill exchange, organize lessons, protect the community and answer data-subject requests.</p>
            <p>We do not sell personal data and we do not use disability, visual markers or income to create a diagnosis. The visual marker is an interface preference and remains private by default.</p>
          </section>
          <section className="space-y-3">
            <h2 className="text-xl font-semibold">2. Optional integrations</h2>
            <p>Video, calendar, notifications, file storage and AI assistants are optional. They should be enabled only for the feature selected by the user, and optional consent can be revoked in the Privacy Center.</p>
            <p>When processing occurs outside Brazil or the European Union, the controller must identify the provider, purpose, destination country, retention period and applicable transfer safeguards.</p>
          </section>
          <section className="space-y-3">
            <h2 className="text-xl font-semibold">3. Your rights</h2>
            <p>Users can access, correct and export their data and request account deactivation. The export includes available account, profile, skills, consent, access-log, post, message and lesson data.</p>
            <p>Some records may be retained in anonymized or restricted form when needed for security, fraud prevention, accountability or a legal obligation.</p>
          </section>
          <section className="space-y-3">
            <h2 className="text-xl font-semibold">4. Minors</h2>
            <p>People under 18 may use the platform. During registration, the user provides a guardian name, email and relationship. The guardian receives a single-use confirmation link, while the account remains accessible with a transparent pending status.</p>
            <p>Email confirmation verifies control of the submitted address, but it does not by itself prove identity or legal authority. Local legal requirements, especially for younger users, must be reviewed before publication in each country.</p>
          </section>
        </>}

        <footer className="border-t border-border pt-6 text-sm text-muted-foreground">
          <p>{language === 'pt' ? `Controlador: ${policy?.controllerName || 'a definir'} · contato: ${policy?.controllerContact || 'a definir antes da publicação'}.` : `Controller: ${policy?.controllerName || 'to be defined'} · contact: ${policy?.controllerContact || 'to be defined before publication'}.`}</p>
          <p className="mt-1">{language === 'pt' ? `Encarregado/DPO: ${policy?.dpoName || 'a definir'} · ${policy?.dpoContact || 'contato a definir'}.` : `DPO: ${policy?.dpoName || 'to be defined'} · ${policy?.dpoContact || 'contact to be defined'}.`}</p>
          <Link to="/login" className="mt-3 inline-block text-skillswap-teal underline">{language === 'pt' ? 'Voltar para o login' : 'Back to login'}</Link>
        </footer>
      </article>
    </main>
  );
};

export default PrivacyPage;
