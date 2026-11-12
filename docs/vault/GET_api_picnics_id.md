<!-- generated:start -->
# GET /api/picnics/{id}

- **Accepts:** path: UUID id
- **Returns:** `ResponseEntity<PicnicResponse>`
- **Status codes:** 200, 404

## Flow

Follow the link to the first step; each step links to the next.

- [[PicnicController.get]] _(controller)_
  - passes `id` → returns `Optional<PicnicRecord>`
    - **PicnicRepository.findById** _(repository)_
      - **Redis READ** `GET` redis:picnic:<id> (PicnicRecord (JSON))
  - passes `record.photoFilename()` → returns `String`
    - **FileSystemStorageService.getFileUrl** _(service)_
<!-- generated:end -->

## Notes

