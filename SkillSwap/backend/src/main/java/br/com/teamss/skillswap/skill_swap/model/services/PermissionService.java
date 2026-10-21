package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.PermissionStatus;

public interface PermissionService {
    void togglePermission(String permissionType, String userId); // Alterna o estado da permissão
    PermissionStatus getPermissionStatus(String permissionType, String userId); // Retorna o status da permissão
    void setPermissionChoice(String permissionType, String userId, String choice); // Define a escolha do usuário
}