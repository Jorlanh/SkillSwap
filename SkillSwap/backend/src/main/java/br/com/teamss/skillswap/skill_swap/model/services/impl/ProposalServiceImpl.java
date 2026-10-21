package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ProposalResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.events.ProposalCompletedEvent; // ADICIONADO
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.ProposalService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher; // ADICIONADO
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProposalServiceImpl implements ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserServiceDTO userServiceDTO;
    
    // REMOVIDO (será movido para o Listener)
    // @Autowired
    // private AchievementService achievementService;

    // ADICIONADO
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Constante (já estava no seu arquivo)
    private static final long VERIFICATION_THRESHOLD = 100;

    @Override
    public Proposal sendProposal(ProposalRequestDTO proposalRequest) {
        User sender = userRepository.findById(proposalRequest.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário remetente não encontrado"));
        
        User receiver = userRepository.findById(proposalRequest.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário destinatário não encontrado"));

        Skill offeredSkill = skillRepository.findById(proposalRequest.getOfferedSkillId())
                .orElseThrow(() -> new EntityNotFoundException("Habilidade oferecida não encontrada"));

        Skill requestedSkill = skillRepository.findById(proposalRequest.getRequestedSkillId())
                .orElseThrow(() -> new EntityNotFoundException("Habilidade solicitada não encontrada"));

        Proposal proposal = new Proposal();
        proposal.setSender(sender);
        proposal.setReceiver(receiver);
        proposal.setOfferedSkill(offeredSkill);
        proposal.setRequestedSkill(requestedSkill);
        proposal.setStatus("PENDING");
        proposal.setCreatedAt(Instant.now());
        proposal.setUpdatedAt(Instant.now());
        
        Proposal savedProposal = proposalRepository.save(proposal);

        String receiverEmail = savedProposal.getReceiver().getEmail();
        String messageContent = "Você recebeu uma proposta de troca de habilidades de " + savedProposal.getSender().getUsername() +
                               ": Oferece " + savedProposal.getOfferedSkill().getName() +
                               " em troca de " + savedProposal.getRequestedSkill().getName();
        
        sendNotification(savedProposal.getReceiver(), receiverEmail, "Nova Proposta no SkillSwap", messageContent);
        
        return savedProposal;
    }

    @Override
    public List<ProposalResponseDTO> getUserProposals(UUID userId) {
        List<Proposal> proposals = proposalRepository.findBySenderIdOrReceiverId(userId);
        
        return proposals.stream().map(proposal -> {
            ProposalResponseDTO dto = new ProposalResponseDTO();
            dto.setProposalId(proposal.getProposalId());
            dto.setSender(new UserSummaryDTO(proposal.getSender().getUsername(), proposal.getSender().getName()));
            dto.setReceiver(new UserSummaryDTO(proposal.getReceiver().getUsername(), proposal.getReceiver().getName()));
            dto.setOfferedSkill(proposal.getOfferedSkill());
            dto.setRequestedSkill(proposal.getRequestedSkill());
            dto.setStatus(proposal.getStatus());
            dto.setCreatedAt(proposal.getCreatedAt());
            dto.setUpdatedAt(proposal.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Proposal acceptProposal(Long proposalId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getReceiver().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("Você não tem permissão para aceitar esta proposta.");
        }
        
        if (!proposal.getStatus().equals("PENDING") && !proposal.getStatus().equals("NEGOTIATING")) {
            throw new IllegalStateException("Esta proposta não pode mais ser aceite.");
        }

        proposal.setStatus("ACCEPTED");
        proposal.setUpdatedAt(Instant.now());
        
        String messageContent = "Boas notícias! " + proposal.getReceiver().getUsername() + " aceitou a sua proposta de troca. A conversa entre vocês já está disponível no chat.";
        sendNotification(proposal.getSender(), proposal.getSender().getEmail(), "Proposta Aceite no SkillSwap!", messageContent);
        
        return proposalRepository.save(proposal);
    }
    
    @Override
    public Proposal negotiateProposal(Long proposalId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getReceiver().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("Você não tem permissão para negociar esta proposta.");
        }

        if (!proposal.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Esta proposta não está mais disponível para negociação.");
        }

        proposal.setStatus("NEGOTIATING");
        proposal.setUpdatedAt(Instant.now());
        
        String messageContent = proposal.getReceiver().getUsername() + " gostaria de negociar os termos da sua proposta. Responda na tela de propostas!";
        sendNotification(proposal.getSender(), proposal.getSender().getEmail(), "Contraproposta no SkillSwap", messageContent);

        return proposalRepository.save(proposal);
    }

    @Override
    public Proposal rejectProposal(Long proposalId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        boolean isSender = proposal.getSender().getUserId().equals(authenticatedUser.getUserId());
        boolean isReceiver = proposal.getReceiver().getUserId().equals(authenticatedUser.getUserId());
        if (!isSender && !isReceiver) {
            throw new AccessDeniedException("Você não tem permissão para rejeitar esta proposta.");
        }

        if (!proposal.getStatus().equals("PENDING") && !proposal.getStatus().equals("NEGOTIATING")) {
            throw new IllegalStateException("Esta proposta não pode mais ser rejeitada.");
        }

        proposal.setStatus("REJECTED");
        proposal.setUpdatedAt(Instant.now());
        
        User userToNotify = isSender ? proposal.getReceiver() : proposal.getSender();
        String messageContent = "A proposta de troca de habilidades foi rejeitada.";
        sendNotification(userToNotify, userToNotify.getEmail(), "Proposta Rejeitada", messageContent);
        
        return proposalRepository.save(proposal);
    }

    @Override
    public Proposal blockProposal(Long proposalId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getReceiver().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("Você não tem permissão para bloquear esta proposta.");
        }

        if (!proposal.getStatus().equals("PENDING") && !proposal.getStatus().equals("NEGOTIATING")) {
            throw new IllegalStateException("Esta proposta não pode mais ser bloqueada.");
        }

        proposal.setStatus("BLOCKED");
        proposal.setUpdatedAt(Instant.now());
        
        String messageContent = "A sua proposta foi bloqueada pelo destinatário. Você não poderá enviar novas propostas para este utilizador.";
        sendNotification(proposal.getSender(), proposal.getSender().getEmail(), "Proposta Bloqueada", messageContent);
        
        return proposalRepository.save(proposal);
    }
    
    @Override
    public Proposal completeProposal(Long proposalId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposta não encontrada."));

        boolean isParticipant = authenticatedUser.getUserId().equals(proposal.getSender().getUserId()) ||
                                authenticatedUser.getUserId().equals(proposal.getReceiver().getUserId());
        if (!isParticipant) {
            throw new AccessDeniedException("Você não tem permissão para concluir esta proposta.");
        }

        if (!"ACCEPTED".equals(proposal.getStatus())) {
            throw new IllegalStateException("Apenas propostas aceites podem ser concluídas.");
        }

        proposal.setStatus("COMPLETED");
        proposal.setUpdatedAt(Instant.now());

        // Ação Síncrona: Salvar no banco (rápido e essencial)
        Proposal savedProposal = proposalRepository.save(proposal);

        // Ação Assíncrona: Disparar o evento
        // O resto (notificações, e-mails, conquistas) acontecerá em background.
        eventPublisher.publishEvent(new ProposalCompletedEvent(this, savedProposal));

        return savedProposal;
    }

    // REMOVIDO: checkAutomaticVerification (movido para o Listener)

    private void sendNotification(User userToNotify, String email, String subject, String message) {
        emailService.sendNotification(email, subject, message);

        Notification notification = new Notification();
        notification.setUser(userToNotify);
        notification.setMessage(message);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}