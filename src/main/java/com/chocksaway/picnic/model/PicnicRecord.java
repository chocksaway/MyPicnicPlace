package com.chocksaway.picnic.model;

import java.io.Serializable;
import java.util.UUID;

/** Flat, serializable form of a {@link Picnic} as stored in Redis and returned by the API. */
public record PicnicRecord(
        UUID id,
        String name,
        double lat,
        double lon,
        String description,
        String photoFilename) implements Serializable {

    public static PicnicRecord from(Picnic picnic) {
        PicnicAttributes a = picnic.getAttributes();
        return new PicnicRecord(a.getId(), picnic.getName(), a.getLat(), a.getLon(),
                picnic.getDescription(), picnic.getPhotoFilename());
    }
}
