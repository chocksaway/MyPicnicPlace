package com.chocksaway.picnic.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test helper showing how to load a classpath resource and convert it to a MultipartFile.
 */
public class MultipartFileFactoryTest {

    @Test
    public void createMultipartFileFromClasspathResource() throws IOException {
        // Resource path on the classpath
        String resourcePath = "/img/chocksaway.png";

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found on classpath: " + resourcePath);

            byte[] content = StreamUtils.copyToByteArray(is);
            assertTrue(content.length > 0, "Loaded resource should not be empty");

            // Create a MockMultipartFile (implements MultipartFile) - useful for tests and controllers
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",                    // form field name
                    "chocksaway.png",          // original filename
                    "image/png",               // content type
                    content                     // file content
            );

            assertNotNull(multipartFile);
            assertEquals("chocksaway.png", multipartFile.getOriginalFilename());
            assertEquals(content.length, multipartFile.getSize());
            assertEquals("image/png", multipartFile.getContentType());
        }
    }
}

