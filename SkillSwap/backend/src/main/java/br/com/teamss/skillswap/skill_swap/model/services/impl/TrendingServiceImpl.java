package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrendingServiceImpl implements TrendingService {

    private final PostRepository postRepository;
    private final ShareLinkRepository shareLinkRepository;

    @Autowired
    public TrendingServiceImpl(PostRepository postRepository, ShareLinkRepository shareLinkRepository) {
        this.postRepository = postRepository;
        this.shareLinkRepository = shareLinkRepository;
    }

    @Override
    public List<Post> getTrendingPosts(String period, int limit) {
        Instant startTime = getStartTime(period);
        List<Post> posts = postRepository.findTrendingPosts(startTime);

        Map<Long, Integer> shareClicks = shareLinkRepository.findAllByPost_PostIdIn(
                posts.stream().map(Post::getPostId).collect(Collectors.toList())
        ).stream()
                .collect(Collectors.groupingBy(
                        shareLink -> shareLink.getPost().getPostId(),
                        Collectors.summingInt(ShareLink::getClickCount)
                ));

        return posts.stream()
                .map(post -> {
                    int clicks = shareClicks.getOrDefault(post.getPostId(), 0);
                    double score = (post.getLikesCount() * 0.25) +
                                   (post.getCommentsCount() * 0.25) +
                                   (post.getRepostsCount() * 0.25) +
                                   (clicks * 0.25);
                    post.setTrendingScore(score);
                    return post;
                })
                .sorted(Comparator.comparingDouble(Post::getTrendingScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Instant getStartTime(String period) {
        switch (period.toUpperCase()) {
            case "DAY":
                return Instant.now().minusSeconds(86400); // 24 horas
            case "WEEK":
                return Instant.now().minusSeconds(604800); // 7 dias
            case "MONTH":
                return Instant.now().minusSeconds(2592000); // 30 dias
            default:
                return Instant.now().minusSeconds(86400);
        }
    }
}