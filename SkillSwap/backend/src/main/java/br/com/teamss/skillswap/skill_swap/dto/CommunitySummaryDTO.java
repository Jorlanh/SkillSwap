package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

/** Dados mínimos usados nas listas públicas de comunidades. */
public record CommunitySummaryDTO(UUID id, String name, String description, int memberCount, String imageUrl, UUID ownerId,
                                  boolean privateCommunity, boolean joined, String membershipStatus) {
}
