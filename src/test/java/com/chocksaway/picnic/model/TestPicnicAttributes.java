package com.chocksaway.picnic.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestPicnicAttributes {
    @Test
    public void testPicnicAttributes() {
        PicnicAttributes picnicAttributes = new PicnicAttributes().build();
        assert picnicAttributes != null;
        assertNotNull(picnicAttributes.getName(), "name should not be null");
    }
}
