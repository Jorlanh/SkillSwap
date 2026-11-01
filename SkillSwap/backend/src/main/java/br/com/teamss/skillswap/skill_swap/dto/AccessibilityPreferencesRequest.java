package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccessibilityPreferencesRequest(
    @NotBlank @Pattern(regexp = "default|high-contrast") String contrastMode,
    @NotBlank @Pattern(regexp = "small|medium|large") String fontScale,
    @NotBlank @Pattern(regexp = "text|audio|video|any") String preferredChannel,
    boolean captionsEnabled,
    boolean imageDescriptionsEnabled,
    boolean simplifiedLanguageEnabled,
    boolean librasEnabled,
    boolean keyboardNavigationEnabled,
    @NotBlank @Pattern(regexp = "standard|focus|contrast") String visualMarker
) {}
