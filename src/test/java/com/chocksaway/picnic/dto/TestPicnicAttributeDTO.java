package com.chocksaway.picnic.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;

import static org.junit.jupiter.api.Assertions.*;

public class TestPicnicAttributeDTO {
    private static ValidatorFactory factory;
    private static Validator validator;

    private final UUID uuid = UUID.randomUUID();

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validDtoHasNoViolations() {
        PicnicAttributeDTO dto = new PicnicAttributeDTO(uuid, 1, 1);
        Set<ConstraintViolation<PicnicAttributeDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void negativeLongitudeIsValid() {
        PicnicAttributeDTO dto = new PicnicAttributeDTO(uuid, 51.5073, -0.1657);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void missingLatLongProduceViolations() {
        PicnicAttributeDTO dto = new PicnicAttributeDTO();
        dto.setId(uuid);
        assertEquals(2, validator.validate(dto).size());
    }

    @Test
    void outOfRangeLatLongProduceViolations() {
        PicnicAttributeDTO dto = new PicnicAttributeDTO(uuid, -95, 190);
        Set<ConstraintViolation<PicnicAttributeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        // Expect two violations: lat and lon
        assertEquals(2, violations.size());
        boolean foundLat = violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("lat"));
        boolean foundLon =  violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("lon"));
        assertTrue(foundLat);
        assertTrue(foundLon);
    }
}

