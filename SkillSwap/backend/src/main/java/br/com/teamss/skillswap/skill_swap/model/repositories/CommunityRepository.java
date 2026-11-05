package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {
    List<Community> findTop50ByNameContainingIgnoreCase(String name);
}
