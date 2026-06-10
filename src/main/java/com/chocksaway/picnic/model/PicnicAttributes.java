package com.chocksaway.picnic.model;

import lombok.Getter;
import java.util.UUID;

public class PicnicAttributes {
    @Getter
    public final UUID name;
    public final double lat;
    public final double lon;

    public PicnicAttributes build() {
        return this;
    }

    public PicnicAttributes() {
        this.name = UUID.randomUUID();
        this.lat =  0.0;
        this.lon = 0.0;
    }

}
