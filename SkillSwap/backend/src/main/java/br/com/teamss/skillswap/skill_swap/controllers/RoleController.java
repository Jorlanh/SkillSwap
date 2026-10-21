package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.teamss.skillswap.skill_swap.dto.RoleDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.services.RoleService;
import br.com.teamss.skillswap.skill_swap.model.services.RoleServiceDTO;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final RoleServiceDTO roleServiceDTO;

    public RoleController(RoleService roleService, RoleServiceDTO roleServiceDTO) {
        this.roleService = roleService;
        this.roleServiceDTO = roleServiceDTO;
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleServiceDTO.findAllDTO());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleServiceDTO.findByIdDTO(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDTO> createRole(@RequestBody Role role) {
        Role savedRole = roleService.save(role);
        RoleDTO roleDTO = roleServiceDTO.toRoleDTO(savedRole);
        return ResponseEntity.ok(roleDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setRoleId(id);
        Role updatedRole = roleService.save(role);
        RoleDTO roleDTO = roleServiceDTO.toRoleDTO(updatedRole);
        return ResponseEntity.ok(roleDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}