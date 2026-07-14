package com.chocksaway.picnic.model;

public class Picnic {
    private final String name;
    private final PicnicAttributes attributes;

    public Picnic(String name, PicnicAttributes attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return this.name;
    }

    public PicnicAttributes getAttributes() {
        return this.attributes;
    }
}
