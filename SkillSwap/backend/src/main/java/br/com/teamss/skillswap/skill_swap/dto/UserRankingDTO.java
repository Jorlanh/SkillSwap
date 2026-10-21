package br.com.teamss.skillswap.skill_swap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserRankingDTO {
    private UserSummaryDTO user;
    private double averageStars;
    private long ratingCount;
    private int rank;
}