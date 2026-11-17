package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

/** Retorno mínimo e seguro após o envio de uma proposta. */
public record ProposalCreatedResponse(
    Long proposalId,
    String status,
    String message,
    Instant createdAt
) {}
