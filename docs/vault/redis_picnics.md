<!-- generated:start -->
# redis:picnics

## Read by

- [[PicnicRepository.findAll]] (SMEMBERS, set of ids)

## Written by

- [[PicnicRepository.save]] (SADD (MULTI/EXEC), set of ids)
- [[PicnicRepository.deleteById]] (SREM (MULTI/EXEC), set of ids)
<!-- generated:end -->

## Notes

