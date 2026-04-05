package com.videostation.application;

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
        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Path.of(originalsPath, storedFileName);

        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }

        return filePath;
    }
}
