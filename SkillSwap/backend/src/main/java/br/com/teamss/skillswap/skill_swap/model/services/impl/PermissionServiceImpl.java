package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.PermissionService;
import br.com.teamss.skillswap.skill_swap.model.entities.Permission;
import br.com.teamss.skillswap.skill_swap.model.entities.PermissionStatus;
import br.com.teamss.skillswap.skill_swap.model.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant; // ALTERADO
import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void togglePermission(String permissionType, String userId) {
        UUID uuid = UUID.fromString(userId);
        Optional<Permission> optionalPermission = permissionRepository.findByUserIdAndPermissionType(uuid, permissionType);

        Permission permission;
        if (optionalPermission.isPresent()) {
            permission = optionalPermission.get();
            PermissionStatus currentStatus = permission.getStatus();
            PermissionStatus newStatus = currentStatus == PermissionStatus.NOT_SET ? PermissionStatus.ALLOWED : PermissionStatus.NOT_SET;
            permission.setStatus(newStatus);
            permission.setUpdatedAt(Instant.now()); // ALTERADO
        } else {
            permission = new Permission(uuid, permissionType, PermissionStatus.ALLOWED);
        }
        permissionRepository.save(permission);
    }

    @Override
    public PermissionStatus getPermissionStatus(String permissionType, String userId) {
        UUID uuid = UUID.fromString(userId);
        Optional<Permission> optionalPermission = permissionRepository.findByUserIdAndPermissionType(uuid, permissionType);
        return optionalPermission.map(Permission::getStatus).orElse(PermissionStatus.NOT_SET);
    }

    @Override
    public void setPermissionChoice(String permissionType, String userId, String choice) {
        UUID uuid = UUID.fromString(userId);
        Optional<Permission> optionalPermission = permissionRepository.findByUserIdAndPermissionType(uuid, permissionType);

        Permission permission;
        if (optionalPermission.isPresent()) {
            permission = optionalPermission.get();
        } else {
            permission = new Permission(uuid, permissionType, PermissionStatus.NOT_SET);
        }

        PermissionStatus newStatus = switch (choice.toLowerCase()) {
            case "allow" -> PermissionStatus.ALLOWED;
            case "allow_during_use" -> PermissionStatus.ALLOWED_DURING_USE;
            case "deny" -> PermissionStatus.DENIED;
            default -> PermissionStatus.NOT_SET;
        };
        permission.setStatus(newStatus);
        permission.setUpdatedAt(Instant.now()); // ALTERADO
        permissionRepository.save(permission);
    }
}