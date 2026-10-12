package com.chocksaway.picnic.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PicnicAttributeDTO {
    @NotNull
    private UUID id;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
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
