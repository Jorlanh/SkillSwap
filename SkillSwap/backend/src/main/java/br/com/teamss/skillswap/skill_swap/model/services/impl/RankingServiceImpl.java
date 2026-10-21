package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.RatingRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserRankingDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.Rating;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RatingRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.RankingService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl implements RankingService {

    private final RatingRepository ratingRepository;
    private final ProposalRepository proposalRepository;
    private final UserServiceDTO userServiceDTO;
    private final UserRepository userRepository;
    private final SwapCoinWalletService walletService;

    public RankingServiceImpl(RatingRepository ratingRepository, ProposalRepository proposalRepository,
                              UserServiceDTO userServiceDTO, UserRepository userRepository, SwapCoinWalletService walletService) {
        this.ratingRepository = ratingRepository;
        this.proposalRepository = proposalRepository;
        this.userServiceDTO = userServiceDTO;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @Override
    @Transactional
    public Rating submitRating(RatingRequestDTO ratingRequestDTO) {
        UserDTO raterUserDTO = userServiceDTO.getAuthenticatedUser();
        Proposal proposal = proposalRepository.findById(ratingRequestDTO.getProposalId())
                .orElseThrow(() -> new IllegalStateException("Proposta não encontrada."));

        if (!"COMPLETED".equals(proposal.getStatus())) {
            throw new IllegalStateException("Só é possível avaliar propostas concluídas.");
        }

        UUID raterId = raterUserDTO.getUserId();
        boolean isParticipant = raterId.equals(proposal.getSender().getUserId()) || raterId.equals(proposal.getReceiver().getUserId());
        if (!isParticipant) {
            throw new AccessDeniedException("Você não tem permissão para avaliar esta troca.");
        }

        User ratedUser = raterId.equals(proposal.getSender().getUserId()) ? proposal.getReceiver() : proposal.getSender();
        if (raterId.equals(ratedUser.getUserId())) {
            throw new IllegalStateException("Não é possível avaliar a si mesmo.");
        }
        
        // CORREÇÃO APLICADA AQUI
        if (ratingRepository.existsByProposal_ProposalIdAndRaterUser_UserId(proposal.getProposalId(), raterId)) {
            throw new IllegalStateException("Você já avaliou esta troca de habilidades.");
        }

        Rating rating = new Rating();
        rating.setProposal(proposal);
        rating.setRaterUser(userRepository.findById(raterId).get());
        rating.setRatedUser(ratedUser);
        rating.setStars(ratingRequestDTO.getStars());
        rating.setComment(ratingRequestDTO.getComment());

        boolean firstFiveStarExchange = rating.getStars() != null && rating.getStars() == 5
            && !ratingRepository.existsByRatedUser_UserIdAndStars(ratedUser.getUserId(), 5);
        Rating saved = ratingRepository.save(rating);
        if (firstFiveStarExchange) {
            walletService.grantAchievementReward(ratedUser.getUserId(), 5,
                "Recompensa por avaliação de 5 estrelas", "rating5:" + proposal.getProposalId() + ":" + ratedUser.getUserId());
        }
        return saved;
    }

    @Override
    public List<UserRankingDTO> getRankings(Optional<Long> skillId) {
        List<Object[]> results = skillId.isPresent()
                ? ratingRepository.findUserRankingsBySkill(skillId.get())
                : ratingRepository.findGeneralUserRankings();

        AtomicInteger rank = new AtomicInteger(1);

        return results.stream().map(result -> {
            UUID userId = (UUID) result[0];
            double averageStars = (Double) result[1];
            long ratingCount = (Long) result[2];
            
            UserSummaryDTO userSummary = userRepository.findById(userId)
                    .map(user -> new UserSummaryDTO(user.getUsername(), user.getName()))
                    .orElse(null);

            return new UserRankingDTO(userSummary, averageStars, ratingCount, rank.getAndIncrement());
        }).collect(Collectors.toList());
    }
}
