<!-- generated:start -->
# POST /api/picnics

- **Accepts:** body: CreatePicnicRequest request
- **Returns:** `ResponseEntity<PicnicResponse>`
- **Status codes:** 201

## Flow

Follow the link to the first step; each step links to the next.

- [[PicnicController.create]] _(controller)_
  - passes `record` → returns `void`
    - **PicnicRepository.save** _(repository)_
      - **Redis WRITE** `SET (MULTI/EXEC)` redis:picnic:<id> (PicnicRecord (JSON))
      - **Redis WRITE** `SADD (MULTI/EXEC)` redis:picnics (set of ids)
  - passes `record.photoFilename()` → returns `String`
    - **FileSystemStorageService.getFileUrl** _(service)_
<!-- generated:end -->

## Notes

