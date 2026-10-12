package com.chocksaway.picnic.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.chocksaway.picnic.AbstractRedisIT;
import com.chocksaway.picnic.image.ImageMetadata;
import com.chocksaway.picnic.image.ImageMetadataRepository;
import com.jayway.jsonpath.JsonPath;

class ImageUploadIT extends AbstractRedisIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ImageMetadataRepository imageMetadataRepository;

    private static MockMultipartFile png() throws Exception {
        try (InputStream in = new ClassPathResource("img/chocksaway.png").getInputStream()) {
            return new MockMultipartFile("file", "chocksaway.png", "image/png", in.readAllBytes());
        }
    }

    @Test
    void uploadStoresFileAndMetadata() throws Exception {
        MockMultipartFile file = png();

        String body = mvc.perform(multipart("/api/upload").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String filename = JsonPath.read(body, "$.filename");
        String url = JsonPath.read(body, "$.url");

        ImageMetadata metadata = imageMetadataRepository.findById(filename).orElseThrow();
        assertEquals("chocksaway.png", metadata.originalFilename());
        assertEquals("image/png", metadata.contentType());
        assertEquals(file.getSize(), metadata.size());
        assertNotNull(metadata.uploadedAt());

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().bytes(file.getBytes()));
    }

    @Test
    void nonImageIsRejectedAndNothingStored() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mvc.perform(multipart("/api/upload").file(text)).andExpect(status().isUnsupportedMediaType());

        assertTrue(redis.keys("image:*").isEmpty());
    }
}
