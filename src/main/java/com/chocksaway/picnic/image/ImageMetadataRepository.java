package com.chocksaway.picnic.image;

import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImageMetadataRepository {

    private static final String KEY_PREFIX = "image:";

    private final RedisTemplate<String, ImageMetadata> redisTemplate;

    public ImageMetadataRepository(RedisTemplate<String, ImageMetadata> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(ImageMetadata metadata) {
        redisTemplate.opsForValue().set(KEY_PREFIX + metadata.id(), metadata);
    }

    public Optional<ImageMetadata> findById(String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + id));
    }

    public void deleteById(String id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }
}