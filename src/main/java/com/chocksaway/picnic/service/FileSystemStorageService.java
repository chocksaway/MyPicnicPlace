package com.chocksaway.picnic.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.chocksaway.picnic.image.ImageMetadata;
import com.chocksaway.picnic.image.ImageMetadataRepository;

@Service
public class FileSystemStorageService {

    private final Path uploadDir;
    private final ImageMetadataRepository imageMetadataRepository;

    public FileSystemStorageService(@Value("${upload.dir}") String uploadDirProp,
                                     ImageMetadataRepository imageMetadataRepository) throws IOException {
        this.uploadDir = Paths.get(uploadDirProp).toAbsolutePath().normalize();
        this.imageMetadataRepository = imageMetadataRepository;
        Files.createDirectories(this.uploadDir);
    }

    /**
     * Store the provided multipart file into the configured upload directory.
     * Returns the generated filename (a UUID with original extension when present).
     */
    public String store(MultipartFile file) throws IOException {
        String originalRaw = file.getOriginalFilename();
        String original = originalRaw == null ? "" : StringUtils.cleanPath(originalRaw);
        if (original.contains("..")) {
            throw new IOException("Invalid path sequence in filename: " + original);
        }

        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx != -1) {
            ext = original.substring(idx);
        }

        String filename = UUID.randomUUID() + ext;
        Path target = this.uploadDir.resolve(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        ImageMetadata metadata = new ImageMetadata(
                filename,
                original,
                file.getContentType(),
                file.getSize(),
                filename,
                Instant.now());
        imageMetadataRepository.save(metadata);

        return filename;
    }

    public Resource loadAsResource(String filename) throws IOException {
        Path file = uploadDir.resolve(filename).normalize();
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new IOException("File not found: " + filename);
        }
        return new UrlResource(file.toUri());
    }

    public String getFileUrl(String filename) {
        // WebConfig maps "/uploads/**" to the upload dir; return that path
        return "/uploads/" + filename;
    }
}

