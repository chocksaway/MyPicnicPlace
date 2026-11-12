<!-- generated:start -->
# FileSystemStorageService.store

- **Layer:** service
- **Signature:** `String store(MultipartFile file)`

## Part of

- [[FileSystemStorageService]]

## Called by

- [[ImageUploadController.handleFileUpload]] passing `file`

## Calls

- [[ImageMetadataRepository.save]] passing `metadata`, returns `void`

## Touches stores

- **filesystem WRITE** `Files.copy` [[filesystem_upload.dir|filesystem:upload.dir]] (file)
<!-- generated:end -->

## Notes

