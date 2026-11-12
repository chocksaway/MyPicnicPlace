<!-- generated:start -->
# PicnicController.create

- **Layer:** controller
- **Signature:** `ResponseEntity<PicnicResponse> create(CreatePicnicRequest request)`

## Exposed as

- [[POST_api_picnics|POST /api/picnics]]

## Calls

- [[PicnicRepository.save]] passing `record`, returns `void`
- [[FileSystemStorageService.getFileUrl]] passing `record.photoFilename()`, returns `String`
<!-- generated:end -->

## Notes

