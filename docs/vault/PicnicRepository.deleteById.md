<!-- generated:start -->
# PicnicRepository.deleteById

- **Layer:** repository
- **Signature:** `void deleteById(UUID id)`

## Part of

- [[PicnicRepository]]

## Touches stores

- **Redis WRITE** `DEL (MULTI/EXEC)` [[redis_picnic_id|redis:picnic:<id>]] (PicnicRecord (JSON))
- **Redis WRITE** `SREM (MULTI/EXEC)` [[redis_picnics|redis:picnics]] (set of ids)
<!-- generated:end -->

## Notes

