package com.chocksaway.picnic.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.chocksaway.picnic.dto.CreatePicnicRequest;
import com.chocksaway.picnic.dto.PicnicResponse;
import com.chocksaway.picnic.model.PicnicRecord;
import com.chocksaway.picnic.repository.PicnicRepository;
import com.chocksaway.picnic.service.FileSystemStorageService;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PicnicControllerTest {

    private final PicnicRepository repository = mock(PicnicRepository.class);
    private final FileSystemStorageService storageService = mock(FileSystemStorageService.class);
    private final PicnicController controller = new PicnicController(repository, storageService);

    @BeforeEach
    void setUp() {
        when(storageService.getFileUrl(anyString())).thenAnswer(inv -> "/uploads/" + inv.getArgument(0));
    }

    @Test
    void createSavesAndReturns201() {
        var request = new CreatePicnicRequest("Hyde Park", 51.5073, -0.1657, "Near the Serpentine", "abc.png");
        ResponseEntity<PicnicResponse> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PicnicResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("Hyde Park", body.name());
        assertEquals(-0.1657, body.lon());
        assertEquals("/uploads/abc.png", body.photoUrl());
        assertEquals("/api/picnics/" + body.id(), response.getHeaders().getLocation().toString());

        // The stored record keeps the raw filename; only the response carries the URL
        ArgumentCaptor<PicnicRecord> saved = ArgumentCaptor.forClass(PicnicRecord.class);
        verify(repository).save(saved.capture());
        assertEquals(body.id(), saved.getValue().id());
        assertEquals("abc.png", saved.getValue().photoFilename());
    }

    @Test
    void getReturns404WhenMissing() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.get(UUID.randomUUID()).getStatusCode());
    }

    @Test
    void getReturnsResponseWithPhotoUrl() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new PicnicRecord(id, "x", 1, 2, "d", "abc.png")));
        assertEquals(new PicnicResponse(id, "x", 1, 2, "d", "/uploads/abc.png"), controller.get(id).getBody());
    }

    @Test
    void listMapsRecordsAndLeavesPhotoUrlNullWithoutPhoto() {
        UUID id = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of(new PicnicRecord(id, "x", 1, 2, "d", null)));
        assertEquals(List.of(new PicnicResponse(id, "x", 1, 2, "d", null)), controller.list());
    }

    @Test
    void requestValidationRejectsBadInput() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertTrue(validator.validate(new CreatePicnicRequest("ok", 51.5, -0.16, null, null)).isEmpty());
            assertEquals(3, validator.validate(new CreatePicnicRequest(" ", 95.0, 190.0, null, null)).size());
            // Missing coordinates must be rejected, not defaulted to 0,0
            assertEquals(2, validator.validate(new CreatePicnicRequest("ok", null, null, null, null)).size());
        }
    }
}
