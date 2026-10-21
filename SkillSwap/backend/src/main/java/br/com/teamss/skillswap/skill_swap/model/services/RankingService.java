package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.RatingRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserRankingDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Rating;

import java.util.List;
import java.util.Optional;

public interface RankingService {
    Rating submitRating(RatingRequestDTO ratingRequestDTO);
    List<UserRankingDTO> getRankings(Optional<Long> skillId);
}