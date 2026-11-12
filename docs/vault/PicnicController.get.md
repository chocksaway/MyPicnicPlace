<!-- generated:start -->
# PicnicController.get

- **Layer:** controller
- **Signature:** `ResponseEntity<PicnicResponse> get(UUID id)`

## Exposed as

- [[GET_api_picnics_id|GET /api/picnics/{id}]]

## Calls

- [[PicnicRepository.findById]] passing `id`, returns `Optional<PicnicRecord>`
- [[FileSystemStorageService.getFileUrl]] passing `record.photoFilename()`, returns `String`
<!-- generated:end -->

## Notes

