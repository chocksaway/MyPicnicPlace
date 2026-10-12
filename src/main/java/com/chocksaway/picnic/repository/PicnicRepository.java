package com.chocksaway.picnic.repository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.stereotype.Repository;

import com.chocksaway.picnic.model.PicnicRecord;

/**
 * Stores each picnic as JSON under {@code picnic:<id>} and keeps a {@code picnics} set of ids for listing.
 * Both are written in one MULTI/EXEC transaction, so a picnic is never stored without being listed (or vice versa).
 */
@Repository
public class PicnicRepository {

    private static final String KEY_PREFIX = "picnic:";
    private static final String INDEX_KEY = "picnics";

    private final StringRedisTemplate redisTemplate;
    private final JacksonJsonRedisSerializer<PicnicRecord> serializer =
            new JacksonJsonRedisSerializer<>(PicnicRecord.class);

    public PicnicRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(PicnicRecord picnic) {
        String id = picnic.id().toString();
        String json = toJson(picnic);
        inTransaction(ops -> {
            ops.opsForValue().set(KEY_PREFIX + id, json);
            ops.opsForSet().add(INDEX_KEY, id);
        });
    }

    public Optional<PicnicRecord> findById(UUID id) {
        return Optional.ofNullable(fromJson(redisTemplate.opsForValue().get(KEY_PREFIX + id)));
    }

    public List<PicnicRecord> findAll() {
        Set<String> ids = redisTemplate.opsForSet().members(INDEX_KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        List<String> found = redisTemplate.opsForValue().multiGet(keys);
        return found == null ? List.of() : found.stream().map(this::fromJson).filter(Objects::nonNull).toList();
    }

    public void deleteById(UUID id) {
        inTransaction(ops -> {
            ops.delete(KEY_PREFIX + id);
            ops.opsForSet().remove(INDEX_KEY, id.toString());
        });
    }

    private void inTransaction(TransactionBody body) {
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
                RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                ops.multi();
                body.apply(ops);
                return ops.exec();
            }
        });
    }

    private String toJson(PicnicRecord picnic) {
        return new String(serializer.serialize(picnic), StandardCharsets.UTF_8);
    }

    private PicnicRecord fromJson(String json) {
        return json == null ? null : serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface TransactionBody {
        void apply(RedisOperations<String, String> ops);
    }
}
