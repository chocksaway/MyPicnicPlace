<!-- generated:start -->
# ImageMetadataRepository.save

- **Layer:** repository
- **Signature:** `void save(ImageMetadata metadata)`

## Part of

- [[ImageMetadataRepository]]

## Called by

- [[FileSystemStorageService.store]] passing `metadata`

## Touches stores

- **Redis WRITE** `SET` [[redis_image_id|redis:image:<id>]] (ImageMetadata (JSON))
<!-- generated:end -->

## Notes

