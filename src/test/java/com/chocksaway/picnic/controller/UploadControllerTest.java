package com.chocksaway.picnic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.chocksaway.picnic.image.ImageMetadata;
import com.chocksaway.picnic.image.ImageMetadataRepository;
import com.chocksaway.picnic.service.FileSystemStorageService;

public class UploadControllerTest {

    @Test
    public void testImageUploadStoresFileAndReturnsMetadata(@TempDir Path tempDir) throws Exception {
        ImageMetadataRepository imageMetadataRepository = mock(ImageMetadataRepository.class);
        FileSystemStorageService storageService = new FileSystemStorageService(tempDir.toString(), imageMetadataRepository);
        ImageUploadController controller = new ImageUploadController(storageService);

        byte[] imageBytes = new byte[] {
                (byte)0xFF, (byte)0xD8, (byte)0xFF
        }; // minimal JPG header bytes

        MultipartFile file = new MultipartFile() {
            @Override
            public String getName() { return "file"; }

            @Override
            public String getOriginalFilename() { return "test.jpg"; }

            @Override
            public String getContentType() { return "image/jpeg"; }

            @Override
            public boolean isEmpty() { return imageBytes.length == 0; }

            @Override
            public long getSize() { return imageBytes.length; }

            @Override
            public byte[] getBytes() throws IOException { return imageBytes; }

            @Override
            public InputStream getInputStream() throws IOException { return new java.io.ByteArrayInputStream(imageBytes); }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                Files.copy(getInputStream(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        };

        // upload image file to a /tmp/??? folder
        var response = controller.handleFileUpload(file);

        // assert response is OK and contains filename and URL
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Object body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        String filename = (String) map.get("filename");
        String url = (String) map.get("url");
        assertNotNull(filename);
        assertNotNull(url);

        Path stored = tempDir.resolve(filename);
        assertTrue(Files.exists(stored), "Uploaded file must exist on disk");
        assertEquals(imageBytes.length, Files.size(stored));

        verify(imageMetadataRepository).save(any(ImageMetadata.class));
    }
}

