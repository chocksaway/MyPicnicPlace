package com.chocksaway.picnic.model;

import java.util.UUID;

public class PicnicAttributes {
    private final UUID id;
    private final double lat;
    private final double lon;

    public PicnicAttributes build() {
        return this;
    }

    public PicnicAttributes() {
        this.id = UUID.randomUUID();
        this.lat =  0.0;
        this.lon = 0.0;
    }

    public UUID getId() {
        return this.id;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }
}
