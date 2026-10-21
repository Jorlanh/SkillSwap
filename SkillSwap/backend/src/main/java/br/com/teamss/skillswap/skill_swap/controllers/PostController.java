package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService;

    public PostController(PostService postService, UserServiceDTO userServiceDTO, FileUploadService fileUploadService) {
        this.postService = postService;
        this.userServiceDTO = userServiceDTO;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(postService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestParam("content") String content, 
                                        @RequestParam(value = "image", required = false) MultipartFile image) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        
        Post newPost = new Post();
        newPost.setContent(content);
        
        try {
            if (image != null && !image.isEmpty()) {
                String imageUrl = fileUploadService.uploadFile(image);
                newPost.setImageUrl(imageUrl);
            }
            // Salvamento real no Banco de Dados
            Post savedPost = postService.save(newPost, authenticatedUser.getUserId()); 
            return ResponseEntity.ok(savedPost);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Falha ao salvar postagem: " + e.getMessage());
        }
    }
}