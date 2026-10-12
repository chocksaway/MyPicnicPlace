package com.chocksaway.picnic.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.chocksaway.picnic.model.PicnicRecord;

@Repository
public class PicnicRepository {

    private static final String KEY_PREFIX = "picnic:";
    private static final String INDEX_KEY = "picnics";

    private final RedisTemplate<String, PicnicRecord> redisTemplate;
    private final StringRedisTemplate indexTemplate;

    public PicnicRepository(RedisTemplate<String, PicnicRecord> picnicRedisTemplate,
                            StringRedisTemplate indexTemplate) {
        this.redisTemplate = picnicRedisTemplate;
        this.indexTemplate = indexTemplate;
    }

    public void save(PicnicRecord picnic) {
        redisTemplate.opsForValue().set(KEY_PREFIX + picnic.id(), picnic);
        indexTemplate.opsForSet().add(INDEX_KEY, picnic.id().toString());
    }

    public Optional<PicnicRecord> findById(UUID id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + id));
    }

    public List<PicnicRecord> findAll() {
        Set<String> ids = indexTemplate.opsForSet().members(INDEX_KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<PicnicRecord> found = redisTemplate.opsForValue().multiGet(keys);
        return found == null ? List.of() : found.stream().filter(Objects::nonNull).toList();
    }

    public void deleteById(UUID id) {
        redisTemplate.delete(KEY_PREFIX + id);
        indexTemplate.opsForSet().remove(INDEX_KEY, id.toString());
    }
}
