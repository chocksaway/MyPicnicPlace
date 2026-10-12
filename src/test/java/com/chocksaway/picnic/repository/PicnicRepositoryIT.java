package com.chocksaway.picnic.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.chocksaway.picnic.AbstractRedisIT;
import com.chocksaway.picnic.model.PicnicRecord;

class PicnicRepositoryIT extends AbstractRedisIT {

    @Autowired
    private PicnicRepository repository;

    private static PicnicRecord picnic(String name) {
        return new PicnicRecord(UUID.randomUUID(), name, 51.5073, -0.1657, "desc", null);
    }

    @Test
    void saveWritesValueAndIndex() {
        PicnicRecord record = picnic("Hyde Park");
        repository.save(record);

        assertTrue(redis.hasKey("picnic:" + record.id()));
        assertTrue(redis.opsForSet().isMember("picnics", record.id().toString()));
    }

    @Test
    void findByIdRoundTripsAllFields() {
        PicnicRecord record = new PicnicRecord(UUID.randomUUID(), "Hyde Park", 51.5073, -0.1657,
                "Near the Serpentine", "abc.png");
        repository.save(record);

        assertEquals(Optional.of(record), repository.findById(record.id()));
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertEquals(Optional.empty(), repository.findById(UUID.randomUUID()));
    }

    @Test
    void findAllReturnsEverySavedPicnic() {
        PicnicRecord a = picnic("A");
        PicnicRecord b = picnic("B");
        repository.save(a);
        repository.save(b);

        assertEquals(Set.of(a, b), Set.copyOf(repository.findAll()));
    }

    @Test
    void findAllIsEmptyWhenNothingSaved() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void deleteRemovesValueAndIndex() {
        PicnicRecord keep = picnic("keep");
        PicnicRecord gone = picnic("gone");
        repository.save(keep);
        repository.save(gone);

        repository.deleteById(gone.id());

        assertFalse(redis.hasKey("picnic:" + gone.id()));
        assertFalse(redis.opsForSet().isMember("picnics", gone.id().toString()));
        assertEquals(java.util.List.of(keep), repository.findAll());
    }
}
