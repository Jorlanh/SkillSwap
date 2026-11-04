package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public record CommunityMemberDTO(UUID userId, String username, String name, String imageUrl,
                                 String role, String status) {}
