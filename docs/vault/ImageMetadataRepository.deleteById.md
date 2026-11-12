<!-- generated:start -->
# ImageMetadataRepository.deleteById

- **Layer:** repository
- **Signature:** `void deleteById(String id)`

## Part of

- [[ImageMetadataRepository]]

## Touches stores

- **Redis WRITE** `DEL` [[redis_image_id|redis:image:<id>]] (ImageMetadata (JSON))
<!-- generated:end -->

## Notes

