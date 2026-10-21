package br.com.teamss.skillswap.skill_swap.dto;

public record ProfileDTO(
    Long profileId,
    String description,
    String imageUrl,
    String location,
    String contactInfo,
    String socialMediaLinks,
    String availabilityStatus,
    String interests,
    String experienceLevel,
    String educationLevel
) {}
