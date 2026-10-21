package br.com.teamss.skillswap.skill_swap.model.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.teamss.skillswap.skill_swap.dto.RoleDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.repositories.RoleRepository;
import br.com.teamss.skillswap.skill_swap.model.services.RoleServiceDTO;
import jakarta.persistence.EntityNotFoundException;

@Service
public class RoleServiceDTOImpl implements RoleServiceDTO {
    
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public RoleDTO toRoleDTO(Role role) {
        return new RoleDTO(role.getRoleId(), role.getName());
    }

    @Override
    public List<RoleDTO> findAllDTO() {
        return roleRepository.findAll().stream()
                .map(this::toRoleDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDTO findByIdDTO(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role não encontrada"));
        return toRoleDTO(role);
    }
}
