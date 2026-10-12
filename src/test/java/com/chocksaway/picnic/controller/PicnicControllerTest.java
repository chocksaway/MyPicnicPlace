package com.chocksaway.picnic.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.chocksaway.picnic.dto.CreatePicnicRequest;
import com.chocksaway.picnic.model.PicnicRecord;
import com.chocksaway.picnic.repository.PicnicRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PicnicControllerTest {

    private final PicnicRepository repository = mock(PicnicRepository.class);
    private final PicnicController controller = new PicnicController(repository);

    @Test
    void createSavesAndReturns201() {
        var request = new CreatePicnicRequest("Hyde Park", 51.5073, -0.1657, "Near the Serpentine", null);
        ResponseEntity<PicnicRecord> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PicnicRecord body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("Hyde Park", body.name());
        assertEquals(-0.1657, body.lon());
        verify(repository).save(body);
        assertEquals("/api/picnics/" + body.id(), response.getHeaders().getLocation().toString());
    }

    @Test
    void getReturns404WhenMissing() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.get(UUID.randomUUID()).getStatusCode());
    }

    @Test
    void getReturnsRecord() {
        UUID id = UUID.randomUUID();
        PicnicRecord record = new PicnicRecord(id, "x", 1, 1, "", null);
        when(repository.findById(id)).thenReturn(Optional.of(record));
        assertEquals(record, controller.get(id).getBody());
    }

    @Test
    void listDelegatesToRepository() {
        PicnicRecord record = new PicnicRecord(UUID.randomUUID(), "x", 1, 1, "", null);
        when(repository.findAll()).thenReturn(List.of(record));
        assertEquals(List.of(record), controller.list());
    }

    @Test
    void requestValidationRejectsBadInput() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertTrue(validator.validate(new CreatePicnicRequest("ok", 51.5, -0.16, null, null)).isEmpty());
            assertEquals(3, validator.validate(new CreatePicnicRequest(" ", 95, 190, null, null)).size());
        }
    }
}
