package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.LikeDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PostService {
    Post createPost(UUID userId, String title, String content, MultipartFile image, MultipartFile video) throws IOException;
    Post save(Post post, UUID userId); // Para compatibilidade com o PostController
    Post likePost(Long postId, UUID userId);
    Post repost(Long postId, UUID userId);
    Post commentOnPost(Long postId, UUID userId, String content);
    String generateShareLink(Long postId);
    List<PostResponseDTO> getPosts(String sortBy, String period);
    List<Post> findAll(); // Para busca geral no PostController
    List<String> getTrendingTopics(String period);
    void incrementViewCount(Long postId);
    List<LikeDTO> getLikesByPost(Long postId);
}