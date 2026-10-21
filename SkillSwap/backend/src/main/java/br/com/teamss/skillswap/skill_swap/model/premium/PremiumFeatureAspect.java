package br.com.teamss.skillswap.skill_swap.model.premium;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.PremiumFeatureService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;

@Aspect
@Component
public class PremiumFeatureAspect {
    private final PremiumFeatureService premiumFeatureService;
    private final UserServiceDTO userService;

    public PremiumFeatureAspect(PremiumFeatureService premiumFeatureService, UserServiceDTO userService) {
        this.premiumFeatureService = premiumFeatureService;
        this.userService = userService;
    }

    @Around("@annotation(feature)")
    public Object charge(ProceedingJoinPoint joinPoint, PremiumFeature feature) throws Throwable {
        UserDTO authenticated = userService.getAuthenticatedUser();
        UUID userId = authenticated.getUserId();
        PremiumCharge charge = premiumFeatureService.consume(userId, feature.code(), feature.cost(), feature.freeQuota());
        try {
            Object result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> response
                && response.getBody() instanceof ChatbotResponseDTO chatbot
                && chatbot.getResponse() != null
                && chatbot.getResponse().toLowerCase().contains("temporariamente indisponível")) {
                premiumFeatureService.refund(charge);
            }
            return result;
        } catch (Throwable failure) {
            premiumFeatureService.refund(charge);
            throw failure;
        }
    }
}
