package com.videostation.application;

import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class FileStorageService {

    @Value("${storage.originals-path}")
    private String originalsPath;

    public Path storeOriginal(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedFileName = UUID.randomUUID() + extension;
        Path filePath = Path.of(originalsPath, storedFileName).normalize();

        if (!filePath.startsWith(Path.of(originalsPath).normalize())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return filePath;
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
