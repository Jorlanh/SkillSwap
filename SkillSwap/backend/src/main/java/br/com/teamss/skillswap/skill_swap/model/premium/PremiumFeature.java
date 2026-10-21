package br.com.teamss.skillswap.skill_swap.model.premium;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an authenticated application operation whose cost is handled by the
 * SwapCoins economy. The aspect is deliberately independent from controllers,
 * so the same policy can be reused by REST, scheduled and message handlers.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PremiumFeature {
    String code();
    long cost() default 0;
    int freeQuota() default 0;
}
