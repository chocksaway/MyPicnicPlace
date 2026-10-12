package com.chocksaway.picnic.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chocksaway.picnic.dto.CreatePicnicRequest;
import com.chocksaway.picnic.model.Picnic;
import com.chocksaway.picnic.model.PicnicAttributes;
import com.chocksaway.picnic.model.PicnicRecord;
import com.chocksaway.picnic.repository.PicnicRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/picnics")
public class PicnicController {

    private final PicnicRepository repository;

    public PicnicController(PicnicRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<PicnicRecord> create(@Valid @RequestBody CreatePicnicRequest request) {
        PicnicAttributes attributes = PicnicAttributes.builder()
                .lat(request.lat())
                .lon(request.lon())
                .build();
        Picnic picnic = new Picnic(request.name(), attributes, request.description(), request.photoFilename());
        PicnicRecord record = PicnicRecord.from(picnic);
        repository.save(record);
        return ResponseEntity.created(URI.create("/api/picnics/" + record.id())).body(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PicnicRecord> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PicnicRecord> list() {
        return repository.findAll();
    }
}
