package com.chocksaway.picnic.model;

import java.util.UUID;

import com.chocksaway.picnic.dto.PicnicAttributeDTO;

public class PicnicAttributes {
    private final UUID id;
    private final double lat;
    private final double lon;

    private PicnicAttributes(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.lat = builder.lat;
        this.lon = builder.lon;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PicnicAttributes from(PicnicAttributeDTO dto) {
        return builder().id(dto.getId()).lat(dto.getLat()).lon(dto.getLon()).build();
    }

    public UUID getId() {
        return this.id;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public static class Builder {
        private UUID id;
        private double lat;
        private double lon;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder lat(double lat) {
            this.lat = lat;
            return this;
        }

        public Builder lon(double lon) {
            this.lon = lon;
            return this;
        }

        public PicnicAttributes build() {
            return new PicnicAttributes(this);
        }
    }
}
