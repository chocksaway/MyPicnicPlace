<!-- generated:start -->
# redis:picnic:<id>

## Read by

- [[PicnicRepository.findById]] (GET, PicnicRecord (JSON))
- [[PicnicRepository.findAll]] (MGET, PicnicRecord (JSON))

## Written by

- [[PicnicRepository.save]] (SET (MULTI/EXEC), PicnicRecord (JSON))
- [[PicnicRepository.deleteById]] (DEL (MULTI/EXEC), PicnicRecord (JSON))
<!-- generated:end -->

## Notes

