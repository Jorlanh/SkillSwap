package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.StoryResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Story;
import br.com.teamss.skillswap.skill_swap.model.repositories.StoryRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService;

    public StoryController(StoryRepository storyRepository, UserRepository userRepository,
                           UserServiceDTO userServiceDTO, FileUploadService fileUploadService) {
        this.storyRepository = storyRepository;
        this.userRepository = userRepository;
        this.userServiceDTO = userServiceDTO;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<StoryResponseDTO>> getActiveStories() {
        Instant now = Instant.now();
        return ResponseEntity.ok(storyRepository.findByExpiresAtAfterOrderByCreatedAtDesc(now)
            .stream().map(this::toDto).toList());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<StoryResponseDTO> createStory(@RequestParam("media") MultipartFile media) throws IOException {
        if (media == null || media.isEmpty()) throw new IllegalArgumentException("Escolha uma foto ou vídeo.");
        String contentType = media.getContentType() == null ? "" : media.getContentType().toLowerCase();
        String mediaType = contentType.startsWith("video/") ? "video" : contentType.startsWith("image/") ? "image" : null;
        if (mediaType == null) throw new IllegalArgumentException("O story precisa ser uma imagem ou um vídeo.");

        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Story story = new Story();
        story.setUser(userRepository.findById(authenticatedUser.getUserId()).orElseThrow());
        story.setMediaUrl(fileUploadService.uploadFile(media));
        story.setMediaType(mediaType);
        story.setCreatedAt(Instant.now());
        story.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        return ResponseEntity.ok(toDto(storyRepository.save(story)));
    }

    private StoryResponseDTO toDto(Story story) {
        return new StoryResponseDTO(story.getStoryId(), story.getUser().getUserId(), story.getUser().getUsername(),
            story.getMediaUrl(), story.getMediaType(), story.getCreatedAt(), story.getExpiresAt());
    }
}
