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
import com.chocksaway.picnic.dto.PicnicResponse;
import com.chocksaway.picnic.model.Picnic;
import com.chocksaway.picnic.model.PicnicAttributes;
import com.chocksaway.picnic.model.PicnicRecord;
import com.chocksaway.picnic.repository.PicnicRepository;
import com.chocksaway.picnic.service.FileSystemStorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/picnics")
public class PicnicController {

    private final PicnicRepository repository;
    private final FileSystemStorageService storageService;

    public PicnicController(PicnicRepository repository, FileSystemStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<PicnicResponse> create(@Valid @RequestBody CreatePicnicRequest request) {
        PicnicAttributes attributes = PicnicAttributes.builder()
                .lat(request.lat())
                .lon(request.lon())
                .build();
        Picnic picnic = new Picnic(request.name(), attributes, request.description(), request.photoFilename());
        PicnicRecord record = PicnicRecord.from(picnic);
        repository.save(record);
        return ResponseEntity.created(URI.create("/api/picnics/" + record.id())).body(toResponse(record));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PicnicResponse> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok(toResponse(record)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PicnicResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private PicnicResponse toResponse(PicnicRecord record) {
        String photoUrl = record.photoFilename() == null ? null : storageService.getFileUrl(record.photoFilename());
        return new PicnicResponse(record.id(), record.name(), record.lat(), record.lon(),
                record.description(), photoUrl);
    }
}
