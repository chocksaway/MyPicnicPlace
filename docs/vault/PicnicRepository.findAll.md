<!-- generated:start -->
# PicnicRepository.findAll

- **Layer:** repository
- **Signature:** `List<PicnicRecord> findAll()`

## Part of

- [[PicnicRepository]]

## Called by

- [[PicnicController.list]] passing ``

## Touches stores

- **Redis READ** `SMEMBERS` [[redis_picnics|redis:picnics]] (set of ids)
- **Redis READ** `MGET` [[redis_picnic_id|redis:picnic:<id>]] (PicnicRecord (JSON))
<!-- generated:end -->

## Notes

