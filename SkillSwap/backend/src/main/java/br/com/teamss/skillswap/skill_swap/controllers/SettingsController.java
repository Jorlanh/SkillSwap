package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus; // Import adicionado
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List; // Import adicionado
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing user settings, including profile, account,
 * privacy, and accessibility preferences.
 */
@Controller // Still assuming server-side rendering with Thymeleaf for some parts
@RequestMapping("/api/settings") // Base path for all settings endpoints
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private UserServiceDTO userServiceDTO;

    @Autowired
    private MessageSource messageSource;

    /**
     * Helper to get the authenticated user's DTO.
     * Throws IllegalStateException if no user is authenticated.
     */
    private UserDTO getAuthenticatedUserDTO() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser")) {
             throw new IllegalStateException("Nenhum usuário autenticado encontrado.");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userServiceDTO.findByUsernameDTO(username);
    }

     /**
      * Adds common model attributes needed for settings pages.
      */
     private void addCommonModelAttributes(Model model, String pageKey, Locale locale) {
         try {
             UserDTO user = getAuthenticatedUserDTO();
             model.addAttribute("user", user); // Add the full UserDTO
             model.addAttribute("pageTitle", messageSource.getMessage(pageKey, null, locale));
         } catch (IllegalStateException e) {
             logger.warn("Tentativa de acesso às configurações sem autenticação.");
              // For server-side views, redirecting might be better if user is definitely needed
              // Consider adding @PreAuthorize("isAuthenticated()") to methods using this helper
              model.addAttribute("user", null);
              model.addAttribute("pageTitle", "Erro de Autenticação"); // Provide a title
         } catch (Exception e) {
             logger.error("Erro ao carregar dados do usuário para as configurações.", e);
             model.addAttribute("user", null);
             model.addAttribute("pageTitle", "Erro Interno"); // Provide a title
         }
     }

    // --- Profile Settings ---

    @GetMapping("/profile") // Changed to specific path
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showProfilePage(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.profileLink", locale);
        return "settings/profile";
    }

    @PostMapping("/profile/update")
    @PreAuthorize("isAuthenticated()") // Ensure user is authenticated
    public String updateProfile(@ModelAttribute("user") UserDTO userDTO, // Use UserDTO for binding
                                RedirectAttributes redirectAttributes, Locale locale) {
        try {
            UserDTO authenticatedUser = getAuthenticatedUserDTO();
            UUID userId = authenticatedUser.getUserId();

            // Basic validation - ensure the DTO ID matches the authenticated user
            if (userDTO.getUserId() == null || !userId.equals(userDTO.getUserId())) {
                 logger.warn("Tentativa de atualização de perfil falhou: ID do DTO ({}) não corresponde ao usuário autenticado ({}).", userDTO.getUserId(), userId);
                 throw new SecurityException("Tentativa de atualização de perfil de outro usuário ou ID ausente.");
            }

            // TODO: Call a specific update method in the service - avoid generic saveDTO
            // userServiceDTO.updateUserProfile(userId, userDTO); // Assume this method exists
            // Example: userService.updateUserBasicProfile(userId, userDTO.getName(), userDTO.getBio(), ...);

            String successMessage = messageSource.getMessage("profile.update.success", null, locale);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            logger.info("Perfil do usuário '{}' (ID: {}) atualizado com sucesso.", authenticatedUser.getUsername(), userId);

        } catch (SecurityException se) {
             logger.error("Falha de segurança ao tentar atualizar perfil: {}", se.getMessage());
             String errorMessage = messageSource.getMessage("error.generic", null, locale); // Generic error for security issues
             redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        } catch (Exception e) {
            logger.error("Erro ao tentar atualizar o perfil do usuário (ID: {}).", userDTO.getUserId(), e);
            String errorMessage = messageSource.getMessage("profile.update.error", null, locale);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
        return "redirect:/api/settings/profile"; // Redirect back to profile page
    }

    // --- Account Settings ---

    @GetMapping("/account")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showAccountPage(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.accountLink", locale);
        return "settings/account";
    }

    // --- Accessibility Settings ---

    @GetMapping("/accessibility")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showAccessibilityPage(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.accessibilityLink", locale); // Ensure this key exists in messages
        return "settings/accessibility"; // Path to your accessibility settings view
    }

    /**
     * Handles updating accessibility settings via API (more suitable for SPAs).
     * Uses PATCH for partial updates.
     */
    @PatchMapping("/accessibility/update")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody // Indicates this method returns data (JSON) directly, not a view name
    public ResponseEntity<?> updateAccessibilitySettings(@RequestBody Map<String, Object> updates, Locale locale) {
        try {
            UserDTO authenticatedUser = getAuthenticatedUserDTO();
            UUID userId = authenticatedUser.getUserId();

            // Extract updates carefully - provide defaults from the authenticated user
            boolean librasEnabled = updates.containsKey("librasAvatarEnabled") ?
                                    (Boolean) updates.get("librasAvatarEnabled") : authenticatedUser.isLibrasAvatarEnabled();
            String theme = updates.containsKey("preferredTheme") ?
                           (String) updates.get("preferredTheme") : authenticatedUser.getPreferredTheme();


            // Validate theme if provided (example validation)
            if (updates.containsKey("preferredTheme") && (theme == null || !List.of("default", "high-contrast-dark", "high-contrast-light").contains(theme))) {
                 logger.warn("Tentativa de definir tema de acessibilidade inválido: {} para usuário {}", theme, userId);
                 return ResponseEntity.badRequest().body(Map.of("error", "Tema inválido selecionado."));
            }

            // Call the specific service method for updating accessibility settings
            userServiceDTO.updateAccessibilitySettingsDTO(userId, librasEnabled, theme);

            logger.info("Configurações de acessibilidade para o usuário '{}' (ID: {}) atualizadas.", authenticatedUser.getUsername(), userId);
            String successMessage = messageSource.getMessage("accessibility.update.success", null, locale); // Need this key in messages
            return ResponseEntity.ok(Map.of("message", successMessage));

        } catch (ClassCastException cce) {
             logger.error("Erro de tipo ao processar atualização de acessibilidade: {}", cce.getMessage());
             String errorMessage = messageSource.getMessage("error.invalidRequestData", null, locale); // Need this key
             return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
        } catch (Exception e) {
            logger.error("Erro ao atualizar configurações de acessibilidade.", e);
            String errorMessage = messageSource.getMessage("accessibility.update.error", null, locale); // Need this key in messages
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", errorMessage));
        }
    }


    // --- Other Settings Pages (Keep similar structure) ---

    @GetMapping("/privacy")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showPrivacyPage(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.privacyLink", locale);
        return "settings/privacy";
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showNotificationsPage(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.notificationsLink", locale);
        return "settings/notifications";
    }

    @GetMapping("/activity")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showActivity(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.activityLink", locale);
        return "settings/activity";
    }

    @GetMapping("/support")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showSupport(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.supportLink", locale);
        return "settings/support";
    }

    @GetMapping("/report")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showReport(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.reportLink", locale);
        return "settings/report";
    }

    @GetMapping("/terms")
    @PreAuthorize("isAuthenticated()") // Secure this page
    public String showTerms(Model model, Locale locale) {
        addCommonModelAttributes(model, "settings.termsLink", locale);
        return "settings/terms";
    }

    // --- Logout ---

    @PostMapping("/logout") // Changed to POST for better security practice
    public String logout(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes, Locale locale) {
         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         String username = (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal())))
                            ? auth.getName() : "desconhecido";
         try {
             if (auth != null) {
                 new SecurityContextLogoutHandler().logout(request, response, auth);
                 logger.info("Usuário '{}' deslogado com sucesso.", username);
             } else {
                 logger.warn("Tentativa de logout sem autenticação prévia.");
             }
             String logoutMessage = messageSource.getMessage("logout.success", null, locale); // Need this key
             redirectAttributes.addFlashAttribute("logoutMessage", logoutMessage);
             return "redirect:/api/login?logout"; // Redirect to login page with logout confirmation
         } catch (Exception e) {
              logger.error("Erro durante o logout do usuário '{}'.", username, e);
              String errorMessage = messageSource.getMessage("logout.error", null, locale); // Need this key
              redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
              return "redirect:/"; // Redirect to homepage or error page on logout failure
         }
    }
}