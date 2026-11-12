<!-- generated:start -->
# filesystem:upload.dir

## Read by

- [[WebConfig.addResourceHandlers]] (serve /uploads/**, file)
- [[FileSystemStorageService.loadAsResource]] (Files.exists, Files.isReadable, file)

## Written by

- [[FileSystemStorageService.store]] (Files.copy, file)
- [[FileSystemStorageService.constructor]] (Files.createDirectories, file)
<!-- generated:end -->

## Notes

