package com.chocksaway.picnic.model;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestPicnicAttributes {
    @Test
    public void testPicnicAttributes() {
        PicnicAttributes picnicAttributes = new PicnicAttributes().build();
        assertNotNull(picnicAttributes);
        assertNotNull(picnicAttributes.getId());
        assertInstanceOf(UUID.class, picnicAttributes.getId());
        assertEquals(0.0, picnicAttributes.getLat());
        assertEquals(0.0, picnicAttributes.getLon());
    }
}