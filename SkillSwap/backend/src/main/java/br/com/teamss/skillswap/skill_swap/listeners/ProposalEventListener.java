package br.com.teamss.skillswap.skill_swap.listeners;

import br.com.teamss.skillswap.skill_swap.events.ProposalCompletedEvent;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import br.com.teamss.skillswap.skill_swap.model.services.impl.SearchServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProposalEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProposalEventListener.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private ProposalRepository proposalRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SearchService searchService; // Para sincronizar com OpenSearch

    private static final long VERIFICATION_THRESHOLD = 100;

    @Async // Executa este método em uma thread separada
    @EventListener
    public void handleProposalCompleted(ProposalCompletedEvent event) {
        Proposal proposal = event.getProposal();
        logger.info("Processando evento de proposta concluída (ID: {}) de forma assíncrona.", proposal.getProposalId());

        try {
            // 1. Enviar notificações e e-mails
            String message = "A troca de habilidades foi concluída! Por favor, avalie a sua experiência para ajudar a comunidade.";
            sendNotification(proposal.getSender(), "Troca Concluída! Hora de avaliar.", message);
            sendNotification(proposal.getReceiver(), "Troca Concluída! Hora de avaliar.", message);

            // 2. Lógica de conquistas e verificação automática
            checkAutomaticVerification(proposal.getSender());
            checkAutomaticVerification(proposal.getReceiver());
            achievementService.checkAndUnlockAchievements(proposal.getSender());
            achievementService.checkAndUnlockAchievements(proposal.getReceiver());
            
            // 3. (Da Etapa 2) Sincronizar com OpenSearch
            // Atualiza o documento da proposta no índice de busca
            // (Você precisará criar um DTO de Proposal para busca ou indexar a entidade)
            // searchService.indexDocument("proposals", proposal.getProposalId().toString(), proposal);
            
            logger.info("Evento de proposta concluída (ID: {}) processado com sucesso.", proposal.getProposalId());
        
        } catch (Exception e) {
            // Se falhar (ex: Twilio offline), apenas registra o erro.
            // Não afeta a transação original do usuário.
            logger.error("Falha ao processar evento de proposta concluída (ID: {}): {}", proposal.getProposalId(), e.getMessage());
        }
    }

    // Lógica movida do ProposalServiceImpl
    private void checkAutomaticVerification(User user) {
        if (user.isVerifiedBadge()) {
            return;
        }
        long completedTrades = proposalRepository.countByStatusAndParticipant("COMPLETED", user.getUserId());
        
        if (completedTrades >= VERIFICATION_THRESHOLD) {
            user.setVerifiedBadge(true);
            User savedUser = userRepository.save(user); // Salva a atualização no usuário

            // Sincroniza usuário atualizado com OpenSearch
            try {
                // (Você precisará de um método para converter User em um DTO de busca simples)
                // Idealmente, crie um DTO UserSearchDTO
                searchService.indexDocument(SearchServiceImpl.USER_INDEX, savedUser.getUserId().toString(), savedUser);
            } catch (Exception e) {
                logger.error("Falha ao sincronizar usuário verificado com OpenSearch: {}", savedUser.getUserId(), e);
            }

            String message = "Parabéns, você completou 100 trocas de habilidades e ganhou o selo de verificado da plataforma!";
            sendNotification(user, "Você é um usuário verificado!", message);
        }
    }

    // Lógica movida do ProposalServiceImpl
    private void sendNotification(User userToNotify, String subject, String message) {
        emailService.sendNotification(userToNotify.getEmail(), subject, message);

        Notification notification = new Notification();
        notification.setUser(userToNotify);
        notification.setMessage(message);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}