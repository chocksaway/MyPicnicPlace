<!-- generated:start -->
# PicnicRepository.findById

- **Layer:** repository
- **Signature:** `Optional<PicnicRecord> findById(UUID id)`

## Part of

- [[PicnicRepository]]

## Called by

- [[PicnicController.get]] passing `id`

## Touches stores

- **Redis READ** `GET` [[redis_picnic_id|redis:picnic:<id>]] (PicnicRecord (JSON))
<!-- generated:end -->

## Notes

