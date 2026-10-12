package com.chocksaway.picnic.dto;

import java.util.UUID;

/**
 * API view of a picnic. Unlike the stored {@code PicnicRecord}, it carries a ready-to-use
 * {@code photoUrl} (null when the picnic has no photo) instead of the raw stored filename.
 */
public record PicnicResponse(
        UUID id,
        String name,
        double lat,
        double lon,
        String description,
        String photoUrl) {
}
