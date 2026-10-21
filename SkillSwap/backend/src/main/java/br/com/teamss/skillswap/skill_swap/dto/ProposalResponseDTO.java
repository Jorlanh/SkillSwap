package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import java.time.Instant;

// DTO para exibir uma Proposta de forma segura
public class ProposalResponseDTO {

    private Long proposalId;
    private UserSummaryDTO sender; // Usa o DTO seguro
    private UserSummaryDTO receiver; // Usa o DTO seguro
    private Skill offeredSkill;
    private Skill requestedSkill;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters e Setters

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public UserSummaryDTO getSender() {
        return sender;
    }

    public void setSender(UserSummaryDTO sender) {
        this.sender = sender;
    }

    public UserSummaryDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(UserSummaryDTO receiver) {
        this.receiver = receiver;
    }

    public Skill getOfferedSkill() {
        return offeredSkill;
    }

    public void setOfferedSkill(Skill offeredSkill) {
        this.offeredSkill = offeredSkill;
    }

    public Skill getRequestedSkill() {
        return requestedSkill;
    }

    public void setRequestedSkill(Skill requestedSkill) {
        this.requestedSkill = requestedSkill;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}