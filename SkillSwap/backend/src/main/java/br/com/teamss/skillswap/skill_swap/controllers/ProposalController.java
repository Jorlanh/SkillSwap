package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ProposalResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.services.ProposalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proposal")
public class ProposalController {

    @Autowired
    private ProposalService proposalService;

    @PostMapping("/send")
    public ResponseEntity<Proposal> sendProposal(@Valid @RequestBody ProposalRequestDTO proposalRequest) {
        Proposal savedProposal = proposalService.sendProposal(proposalRequest);
        return ResponseEntity.ok(savedProposal);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProposalResponseDTO>> getUserProposals(@PathVariable UUID userId) {
        List<ProposalResponseDTO> proposals = proposalService.getUserProposals(userId);
        return ResponseEntity.ok(proposals);
    }

    @PostMapping("/accept/{proposalId}")
    public ResponseEntity<Proposal> acceptProposal(@PathVariable Long proposalId) {
        Proposal updatedProposal = proposalService.acceptProposal(proposalId);
        return ResponseEntity.ok(updatedProposal);
    }

    @PostMapping("/negotiate/{proposalId}")
    public ResponseEntity<Proposal> negotiateProposal(@PathVariable Long proposalId) {
        Proposal updatedProposal = proposalService.negotiateProposal(proposalId);
        return ResponseEntity.ok(updatedProposal);
    }

    @PostMapping("/reject/{proposalId}")
    public ResponseEntity<Proposal> rejectProposal(@PathVariable Long proposalId) {
        Proposal updatedProposal = proposalService.rejectProposal(proposalId);
        return ResponseEntity.ok(updatedProposal);
    }

    @PostMapping("/block/{proposalId}")
    public ResponseEntity<Proposal> blockProposal(@PathVariable Long proposalId) {
        Proposal updatedProposal = proposalService.blockProposal(proposalId);
        return ResponseEntity.ok(updatedProposal);
    }

    // NOVO ENDPOINT ADICIONADO
    @PostMapping("/complete/{proposalId}")
    public ResponseEntity<Proposal> completeProposal(@PathVariable Long proposalId) {
        Proposal updatedProposal = proposalService.completeProposal(proposalId);
        return ResponseEntity.ok(updatedProposal);
    }
}