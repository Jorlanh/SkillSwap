package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/settings/security")
public class SecurityController {

    @Autowired
    private UserServiceDTO userServiceDTO;

    @GetMapping
    public String showSecuritySettings(Model model) {
        try {
            // Obtém o usuário logado do Spring Security
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = userDetails.getUsername();

            // Busca o UserDTO pelo username
            UserDTO user = userServiceDTO.findByUsernameDTO(username);
            model.addAttribute("username", user.getUsername());
            return "seguranca"; // Nome do template (seguranca.html)
        } catch (Exception e) {
            return "redirect:/api/login"; // Redireciona para login se não estiver autenticado
        }
    }

    @GetMapping("/change-password")
    public String showChangePassword() {
        return "redirect:/api/settings/security";
    }

    @GetMapping("/two-factor")
    public String showTwoFactor() {
        return "redirect:/api/settings/security";
    }

    @GetMapping("/access-history")
    public String showAccessHistory() {
        return "redirect:/api/settings/security";
    }

    @GetMapping("/permissions")
    public String showPermissions() {
        return "redirect:/api/settings/security";
    }
}