<!-- generated:start -->
# PicnicController.list

- **Layer:** controller
- **Signature:** `List<PicnicResponse> list()`

## Exposed as

- [[GET_api_picnics|GET /api/picnics]]

## Calls

- [[PicnicRepository.findAll]] passing ``, returns `List<PicnicRecord>`
- [[FileSystemStorageService.getFileUrl]] passing `record.photoFilename()`, returns `String`
<!-- generated:end -->

## Notes

