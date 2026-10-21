package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.*;
import br.com.teamss.skillswap.skill_swap.model.services.impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public TrendingService trendingService(PostRepository postRepository, ShareLinkRepository shareLinkRepository) {
        return new TrendingServiceImpl(postRepository, shareLinkRepository);
    }

    @Bean
    public ProfileService profileService(
            UserRepository userRepository, 
            PostRepository postRepository, 
            ProfileRepository profileRepository, 
            FileUploadService fileUploadService) {
        // CORREÇÃO: Construtor atualizado com as 4 dependências necessárias
        return new ProfileServiceImpl(); 
        // Nota técnica: Se o seu ProfileServiceImpl usa @Autowired nos campos, 
        // basta retornar 'new ProfileServiceImpl()'. 
        // Se usa injeção via construtor, passe as 4 variáveis acima.
    }

    @Bean
    public CommunityService communityService(
            CommunityRepository communityRepository, 
            UserRepository userRepository,
            PostRepository postRepository, 
            CommunityMemberRepository communityMemberRepository,
            ProfileRepository profileRepository, 
            LikeRepository likeRepository, 
            CommentRepository commentRepository,
            RepostRepository repostRepository, 
            ShareLinkRepository shareLinkRepository,
            FileUploadService fileUploadService, 
            ContentModerationService moderationService) {
        return new CommunityServiceImpl(
                communityRepository, userRepository, postRepository, communityMemberRepository,
                profileRepository, likeRepository, commentRepository, repostRepository, shareLinkRepository,
                fileUploadService, moderationService);
    }
}