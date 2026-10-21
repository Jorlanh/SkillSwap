package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ProposalResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import java.util.List;
import java.util.UUID;

public interface ProposalService {
    Proposal sendProposal(ProposalRequestDTO proposalRequest);
    List<ProposalResponseDTO> getUserProposals(UUID userId);
    Proposal acceptProposal(Long proposalId);
    Proposal negotiateProposal(Long proposalId);
    Proposal rejectProposal(Long proposalId);
    Proposal blockProposal(Long proposalId);
    Proposal completeProposal(Long proposalId);
}