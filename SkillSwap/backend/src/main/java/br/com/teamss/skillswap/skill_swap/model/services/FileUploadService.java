package br.com.teamss.skillswap.skill_swap.model.services;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileUploadService {
    /**
     * Faz o upload de um arquivo para o serviço de nuvem.
     * @param file O arquivo a ser enviado.
     * @return A URL segura do arquivo após o upload.
     * @throws IOException se ocorrer um erro durante o upload.
     */
    String uploadFile(MultipartFile file) throws IOException;
}