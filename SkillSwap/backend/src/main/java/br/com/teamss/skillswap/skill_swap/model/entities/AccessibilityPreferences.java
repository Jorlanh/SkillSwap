package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ss_accessibility_preferences")
@Getter
@Setter
@NoArgsConstructor
public class AccessibilityPreferences {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "contrast_mode", nullable = false, length = 24)
    private String contrastMode = "default";

    @Column(name = "font_scale", nullable = false, length = 16)
    private String fontScale = "medium";

    @Column(name = "preferred_channel", nullable = false, length = 16)
    private String preferredChannel = "any";

    @Column(name = "captions_enabled", nullable = false)
    private boolean captionsEnabled;

    @Column(name = "image_descriptions_enabled", nullable = false)
    private boolean imageDescriptionsEnabled = true;

    @Column(name = "simplified_language_enabled", nullable = false)
    private boolean simplifiedLanguageEnabled = true;

    @Column(name = "libras_enabled", nullable = false)
    private boolean librasEnabled;

    @Column(name = "keyboard_navigation_enabled", nullable = false)
    private boolean keyboardNavigationEnabled = true;

    @Column(name = "visual_marker", nullable = false, length = 16)
    private String visualMarker = "standard";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
