package com.chocksaway.picnic.model;

public class Picnic {
    private final String name;
    private final PicnicAttributes attributes;
    private final String description;
    private final String photoFilename;

    public Picnic(String name, PicnicAttributes attributes) {
        this(name, attributes, "", null);
    }

    public Picnic(String name, PicnicAttributes attributes, String description, String photoFilename) {
        this.name = name;
        this.attributes = attributes;
        this.description = description;
        this.photoFilename = photoFilename;
    }

    public String getName() {
        return this.name;
    }

    public PicnicAttributes getAttributes() {
        return this.attributes;
    }

    public String getDescription() {
        return this.description;
    }

    /** Filename of the uploaded image (matches {@code ImageMetadata.id}), or null if none. */
    public String getPhotoFilename() {
        return this.photoFilename;
    }
}
