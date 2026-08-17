package com.chocksaway.picnic.image;

import java.io.Serializable;
import java.time.Instant;

/**
 * Indexed alongside the on-disk file so an image can be looked up by id without
 * scanning the upload directory. The file itself remains the source of truth for bytes.
 */
public record ImageMetadata(
        String id,
        String originalFilename,
        String contentType,
        long size,
        String path,
        Instant uploadedAt) implements Serializable {
}