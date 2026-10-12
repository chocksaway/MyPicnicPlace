package com.chocksaway.picnic.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestPicnicAttributes {
    @Test
    public void defaultsToRandomIdAndZeroCoordinates() {
        PicnicAttributes picnicAttributes = PicnicAttributes.builder().build();
        assertNotNull(picnicAttributes.getId());
        assertEquals(0.0, picnicAttributes.getLat());
        assertEquals(0.0, picnicAttributes.getLon());
    }

    @Test
    public void builderSetsValues() {
        UUID id = UUID.randomUUID();
        PicnicAttributes a = PicnicAttributes.builder().id(id).lat(51.5073).lon(-0.1657).build();
        assertEquals(id, a.getId());
        assertEquals(51.5073, a.getLat());
        assertEquals(-0.1657, a.getLon());
    }
}
