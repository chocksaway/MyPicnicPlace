package com.chocksaway.picnic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;

public class TestPicnicSite {
    @Test
    public void testPicnicSiteBuild() {
        final String name = "myPicnicName";
        PicnicAttributes attributes = PicnicAttributes.builder().build();
        Picnic picnic = new Picnic(name, attributes);
        assertNotNull(picnic);
        assertEquals("The picnic name should be myPicnicName", "myPicnicName",
                picnic.getName());
        assertEquals("The picnic attributes should be the same as the one passed in",
                attributes, picnic.getAttributes());
    }
}
