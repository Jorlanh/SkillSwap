package br.com.teamss.skillswap.skill_swap.listeners;

// ADIÇÃO DE IMPORTS ESTÁTICOS
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException; // ADICIONADO
import java.util.UUID;

// ADIÇÃO DE IMPORTS DE TESTE
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.teamss.skillswap.skill_swap.events.ProposalCompletedEvent;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import br.com.teamss.skillswap.skill_swap.model.services.impl.SearchServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProposalEventListenerTest {

    @Mock
    private EmailService emailService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AchievementService achievementService;
    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SearchService searchService; // Mock do serviço de busca

    @InjectMocks
    private ProposalEventListener proposalEventListener;

    @Test
    void testHandleProposalCompleted() throws InterruptedException, IOException { // Adicionado IOException
        // 1. Setup (Cenário)
        User sender = new User();
        sender.setUserId(UUID.randomUUID());
        sender.setEmail("sender@test.com");
        sender.setVerifiedBadge(false);

        User receiver = new User();
        receiver.setUserId(UUID.randomUUID());
        receiver.setEmail("receiver@test.com");
        receiver.setVerifiedBadge(false);

        Proposal proposal = new Proposal();
        proposal.setProposalId(1L);
        proposal.setSender(sender);
        proposal.setReceiver(receiver);

        ProposalCompletedEvent event = new ProposalCompletedEvent(this, proposal);

        // Define o comportamento dos mocks
        // Cenário: Sender atinge 100 trades, Receiver tem 10 trades.
        when(proposalRepository.countByStatusAndParticipant("COMPLETED", sender.getUserId())).thenReturn(100L);
        when(proposalRepository.countByStatusAndParticipant("COMPLETED", receiver.getUserId())).thenReturn(10L);
        
        // Mock para o save do usuário (quando o sender é verificado)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // 2. Action (Ação)
        // Chamamos o método que (internamente) é @Async
        proposalEventListener.handleProposalCompleted(event);

        // 3. Verification (Verificação)
        // Como o método é @Async, esperamos por ele (timeout de 1-2 segundos)
        
        // Verificando e-mails
        verify(emailService, timeout(2000)).sendNotification(eq("sender@test.com"), anyString(), anyString());
        verify(emailService, timeout(2000)).sendNotification(eq("receiver@test.com"), anyString(), anyString());

        // Verificando conquistas
        verify(achievementService, timeout(2000)).checkAndUnlockAchievements(sender);
        verify(achievementService, timeout(2000)).checkAndUnlockAchievements(receiver);

        // Verificando a lógica de "checkAutomaticVerification"
        // O sender (100 trades) deve ser verificado
        verify(userRepository, timeout(2000)).save(sender);
        
        // CORREÇÃO: O receiver (10 trades) NÃO deve ser salvo (pois não atingiu o threshold)
        // Verificamos que o save foi chamado 1 vez (para o sender), mas nunca para o receiver.
        verify(userRepository, timeout(2000).times(1)).save(any(User.class)); // Garante que o save foi chamado
        
        // **** ESTA É A LINHA CORRIGIDA ****
        verify(userRepository, never()).save(receiver); // Garante que o receiver especificamente não foi salvo
        
        // Verificando sincronização com OpenSearch
        // verify(searchService, timeout(2000)).indexDocument(eq("proposals"), eq("1"), any(Proposal.class));
        
        // Verifica se o usuário verificado (sender) foi re-indexado
        verify(searchService, timeout(2000)).indexDocument(eq(SearchServiceImpl.USER_INDEX), eq(sender.getUserId().toString()), any(User.class));
    }
}