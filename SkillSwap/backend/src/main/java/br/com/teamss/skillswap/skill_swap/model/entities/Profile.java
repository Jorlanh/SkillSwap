package br.com.teamss.skillswap.skill_swap.model.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_profiles")
@Getter
@Setter
@AllArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference 
    private User user;

    private String description;

    private String imageUrl;

    private String location;

    @Column(name = "contact_info")
    private String contactInfo;

    @Column(name = "social_media_links")
    private String socialMediaLinks;

    @Column(name = "availability_status")
    private String availabilityStatus;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private Set<Skill> skills = new HashSet<>();

    private String interests;

    private String experienceLevel;

    private String educationLevel;

    // --- INCREMENTO: CONTA PRIVADA ---
    @Column(name = "is_private")
    private Boolean isPrivate = false;

    @Column(name = "extra_portfolio_slots", nullable = false)
    private int extraPortfolioSlots = 0;

    @Column(name = "extra_bio_chars", nullable = false)
    private int extraBioChars = 0;

    @Column(name = "extra_post_chars", nullable = false)
    private int extraPostChars = 0;
    
    public Profile() {}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((profileId == null) ? 0 : profileId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Profile other = (Profile) obj;
        if (profileId == null) {
            if (other.profileId != null)
                return false;
        } else if (!profileId.equals(other.profileId))
            return false;
        return true;
    }
}
