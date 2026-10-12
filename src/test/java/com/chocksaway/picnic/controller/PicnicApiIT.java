package com.chocksaway.picnic.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.chocksaway.picnic.AbstractRedisIT;

class PicnicApiIT extends AbstractRedisIT {

    @Autowired
    private MockMvc mvc;

    private String postPicnic(String json) throws Exception {
        return mvc.perform(post("/api/picnics").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void createThenGetAndList() throws Exception {
        String location = postPicnic("""
                {"name":"Hyde Park","lat":51.5073,"lon":-0.1657,"description":"Near the Serpentine"}
                """);
        assertNotNull(location);
        assertTrue(location.startsWith("/api/picnics/"));

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hyde Park"))
                .andExpect(jsonPath("$.lat").value(51.5073))
                .andExpect(jsonPath("$.lon").value(-0.1657))
                .andExpect(jsonPath("$.description").value("Near the Serpentine"));

        mvc.perform(get("/api/picnics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Hyde Park"));
    }

    @Test
    void missingLatIsRejectedAndNothingStored() throws Exception {
        mvc.perform(post("/api/picnics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No lat\",\"lon\":-0.1657}"))
                .andExpect(status().isBadRequest());

        assertEquals(0L, redis.opsForSet().size("picnics"));
    }

    @Test
    void outOfRangeCoordinatesAreRejected() throws Exception {
        mvc.perform(post("/api/picnics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nowhere\",\"lat\":95,\"lon\":-0.1657}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mvc.perform(post("/api/picnics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"lat\":51.5,\"lon\":-0.16}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mvc.perform(get("/api/picnics/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void malformedIdReturns400() throws Exception {
        mvc.perform(get("/api/picnics/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void emptyListWhenNothingSaved() throws Exception {
        mvc.perform(get("/api/picnics")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }
}
