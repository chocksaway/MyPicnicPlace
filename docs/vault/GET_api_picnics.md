<!-- generated:start -->
# GET /api/picnics

- **Accepts:** -
- **Returns:** `List<PicnicResponse>`
- **Status codes:** 200

## Flow

Follow the link to the first step; each step links to the next.

- [[PicnicController.list]] _(controller)_
  - passes `` → returns `List<PicnicRecord>`
    - **PicnicRepository.findAll** _(repository)_
      - **Redis READ** `SMEMBERS` redis:picnics (set of ids)
      - **Redis READ** `MGET` redis:picnic:<id> (PicnicRecord (JSON))
  - passes `record.photoFilename()` → returns `String`
    - **FileSystemStorageService.getFileUrl** _(service)_
<!-- generated:end -->

## Notes

