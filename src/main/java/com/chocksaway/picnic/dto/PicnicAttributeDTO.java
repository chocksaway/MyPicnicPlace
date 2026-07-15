package com.chocksaway.picnic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class PicnicAttributeDTO {
    @NotNull
    private UUID id;

    @NotNull
    @Positive
    private double lat;

    @NotNull
    @Positive
    private double lon;


    public PicnicAttributeDTO() {
    }

    public PicnicAttributeDTO(UUID id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
