package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryFileUploadServiceImpl implements FileUploadService {

    @Autowired
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif",
            "video/mp4", "video/mpeg", "video/quicktime"
    );

    private static final long MAX_FILE_SIZE = 10485760; // 10 MB

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de ficheiro inválido. Tipos permitidos são: " + ALLOWED_MIME_TYPES);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O tamanho do arquivo excede o limite de 10MB.");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        return uploadResult.get("secure_url").toString();
    }
}