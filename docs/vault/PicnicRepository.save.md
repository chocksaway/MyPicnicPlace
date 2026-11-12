<!-- generated:start -->
# PicnicRepository.save

- **Layer:** repository
- **Signature:** `void save(PicnicRecord picnic)`

## Part of

- [[PicnicRepository]]

## Called by

- [[PicnicController.create]] passing `record`

## Touches stores

- **Redis WRITE** `SET (MULTI/EXEC)` [[redis_picnic_id|redis:picnic:<id>]] (PicnicRecord (JSON))
- **Redis WRITE** `SADD (MULTI/EXEC)` [[redis_picnics|redis:picnics]] (set of ids)
<!-- generated:end -->

## Notes

