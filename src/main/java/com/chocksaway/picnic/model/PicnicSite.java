package com.chocksaway.picnic.model;

public interface PicnicSite {
    default PicnicSite build(Picnic picnic) {
        return this;
    }



}
