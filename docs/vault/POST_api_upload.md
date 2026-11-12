<!-- generated:start -->
# POST /api/upload

- **Accepts:** param: MultipartFile file
- **Returns:** `ResponseEntity<?>`
- **Status codes:** 200, 400, 415, 500

## Flow

Follow the link to the first step; each step links to the next.

- [[ImageUploadController.handleFileUpload]] _(controller)_
  - passes `file` → returns `String`
    - **FileSystemStorageService.store** _(service)_
      - passes `metadata` → returns `void`
        - **ImageMetadataRepository.save** _(repository)_
          - **Redis WRITE** `SET` redis:image:<id> (ImageMetadata (JSON))
      - **filesystem WRITE** `Files.copy` filesystem:upload.dir (file)
  - passes `filename` → returns `String`
    - **FileSystemStorageService.getFileUrl** _(service)_
<!-- generated:end -->

## Notes

