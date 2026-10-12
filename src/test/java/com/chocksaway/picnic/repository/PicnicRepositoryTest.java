package com.chocksaway.picnic.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.chocksaway.picnic.model.PicnicRecord;

class PicnicRepositoryTest {

    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final RedisOperations<String, String> txOps = mock(RedisOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final SetOperations<String, String> sets = mock(SetOperations.class);

    private final PicnicRepository repository = new PicnicRepository(template);
    private final PicnicRecord record =
            new PicnicRecord(UUID.randomUUID(), "Hyde Park", 51.5073, -0.1657, "Near the Serpentine", null);

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        // Run the repository's SessionCallback against the mocked transactional operations
        when(template.execute(any(SessionCallback.class)))
                .thenAnswer(inv -> ((SessionCallback) inv.getArgument(0)).execute(txOps));
        when(txOps.opsForValue()).thenReturn(values);
        when(txOps.opsForSet()).thenReturn(sets);
        when(template.opsForValue()).thenReturn(values);
        when(template.opsForSet()).thenReturn(sets);
    }

    @Test
    void saveWritesValueAndIndexInOneTransaction() {
        repository.save(record);

        String id = record.id().toString();
        InOrder order = inOrder(txOps, values, sets);
        order.verify(txOps).multi();
        order.verify(values).set(eq("picnic:" + id), anyString());
        order.verify(sets).add("picnics", id);
        order.verify(txOps).exec();
    }

    @Test
    void deleteRemovesValueAndIndexInOneTransaction() {
        repository.deleteById(record.id());

        String id = record.id().toString();
        InOrder order = inOrder(txOps, sets);
        order.verify(txOps).multi();
        order.verify(txOps).delete("picnic:" + id);
        order.verify(sets).remove("picnics", id);
        order.verify(txOps).exec();
    }

    @Test
    void savedJsonRoundTripsThroughFindById() {
        repository.save(record);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(anyString(), json.capture());

        when(values.get("picnic:" + record.id())).thenReturn(json.getValue());
        assertEquals(Optional.of(record), repository.findById(record.id()));
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertEquals(Optional.empty(), repository.findById(UUID.randomUUID()));
    }

    @Test
    void findAllSkipsIdsWithNoValue() {
        repository.save(record);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(anyString(), json.capture());

        UUID orphan = UUID.randomUUID();
        when(sets.members("picnics")).thenReturn(Set.of(record.id().toString(), orphan.toString()));
        when(values.multiGet(any())).thenReturn(java.util.Arrays.asList(json.getValue(), null));

        assertEquals(List.of(record), repository.findAll());
    }

    @Test
    void findAllReturnsEmptyWhenNoIds() {
        when(sets.members("picnics")).thenReturn(Set.of());
        assertEquals(List.of(), repository.findAll());
    }
}
