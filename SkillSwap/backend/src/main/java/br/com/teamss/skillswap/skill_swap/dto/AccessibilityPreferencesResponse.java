package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.AccessibilityPreferences;

public record AccessibilityPreferencesResponse(
    String contrastMode,
    String fontScale,
    String preferredChannel,
    boolean captionsEnabled,
    boolean imageDescriptionsEnabled,
    boolean simplifiedLanguageEnabled,
    boolean librasEnabled,
    boolean keyboardNavigationEnabled,
    String visualMarker
) {
    public static AccessibilityPreferencesResponse from(AccessibilityPreferences p) {
        return new AccessibilityPreferencesResponse(
            p.getContrastMode(), p.getFontScale(), p.getPreferredChannel(),
            p.isCaptionsEnabled(), p.isImageDescriptionsEnabled(),
            p.isSimplifiedLanguageEnabled(), p.isLibrasEnabled(),
            p.isKeyboardNavigationEnabled(), p.getVisualMarker()
        );
    }
}
