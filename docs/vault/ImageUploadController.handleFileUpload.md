<!-- generated:start -->
# ImageUploadController.handleFileUpload

- **Layer:** controller
- **Signature:** `ResponseEntity<?> handleFileUpload(MultipartFile file)`

## Exposed as

- [[POST_api_upload|POST /api/upload]]

## Calls

- [[FileSystemStorageService.store]] passing `file`, returns `String`
- [[FileSystemStorageService.getFileUrl]] passing `filename`, returns `String`
<!-- generated:end -->

## Notes

