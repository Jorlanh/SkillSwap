package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryFileUploadServiceImpl implements FileUploadService {

    @Autowired
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/avif", "image/bmp", "image/heic", "image/heif",
            "video/mp4", "video/mpeg", "video/quicktime"
    );

    private static final long MAX_FILE_SIZE = 10485760; // 10 MB

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (!isAllowedImageOrVideo(contentType, file.getOriginalFilename())) {
            throw new IllegalArgumentException("Tipo de ficheiro inválido. Tipos permitidos são: " + ALLOWED_MIME_TYPES);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O tamanho do arquivo excede o limite de 10MB.");
        }

        Map<?, ?> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        } catch (RuntimeException ex) {
            // Cloudinary can throw a runtime exception when the Railway
            // credentials are missing/invalid. Convert it to the checked
            // contract used by the controllers so the API returns a useful
            // upload error instead of an opaque 500 response.
            throw new IOException("Não foi possível enviar a imagem para o armazenamento.", ex);
        }
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null || secureUrl.toString().isBlank()) {
            throw new IOException("O serviço de imagens não retornou o endereço da foto.");
        }
        return secureUrl.toString();
    }

    private boolean isAllowedImageOrVideo(String contentType, String originalFilename) {
        if (contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) return true;
        if (originalFilename == null) return false;
        String filename = originalFilename.toLowerCase(Locale.ROOT);
        return Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif", ".bmp", ".heic", ".heif", ".mp4", ".mpeg", ".mov")
            .stream().anyMatch(filename::endsWith);
    }
}
