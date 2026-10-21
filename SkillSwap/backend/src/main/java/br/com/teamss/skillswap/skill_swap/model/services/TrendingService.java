package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import java.util.List;

public interface TrendingService {
    List<Post> getTrendingPosts(String period, int limit);
}