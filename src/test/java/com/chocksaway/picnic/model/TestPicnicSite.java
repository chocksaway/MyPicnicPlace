package com.chocksaway.picnic.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPicnicSite {
    @Test
    public void testPicnicSiteBuild() {
        PicnicSite picnicSite = new PicnicSiteImpl().build(new Picnic());
        Assertions.assertNotNull(picnicSite);
    }
}
