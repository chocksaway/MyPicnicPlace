     # CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

MyPicnicPlace is an early-stage Spring Boot 4.0.6 (Java 21) application for cataloguing picnic sites, with image upload, Redis-backed storage of picnic sites and image metadata, and a separate Leaflet map frontend project (`../MyPicnicPlaceFrontend`). Group/artifact: `com.chocksaway:my-picnic-place`. The codebase is small and still taking shape — expect incomplete abstractions rather than settled architecture.

Redis must be running on `localhost:6379` (see `application.properties`) for the app to store uploads and picnic sites. Unit tests do not need Redis.

## Commands

Use the Maven wrapper (`./mvnw`), not a system-installed Maven.

```bash
./mvnw compile                 # compile main sources
./mvnw test                    # run the full test suite
./mvnw test -Dtest=TestPicnicAttributes          # run a single test class
./mvnw test -Dtest=TestPicnicAttributeDTO#negativeLatLongProduceViolations  # run a single test method
./mvnw spring-boot:run          # run the app locally
./mvnw clean package            # build the jar (target/*.jar)
```

There is no linter or formatter configured in the project.

## Architecture

Standard Spring Boot layout under `src/main/java/com/chocksaway/picnic/`:

- `MyPicnicPlace.java` — `@SpringBootApplication` entry point.
- `controller/` — REST controllers. `ImageUploadController` exposes `POST /api/upload` (multipart), validates the file is non-empty and has an `image/*` content type, then delegates storage to `FileSystemStorageService`.
- `controller/PicnicController.java` — `/api/picnics`: `POST` (validated `CreatePicnicRequest`, server generates the id, returns 201 + `Location`), `GET /{id}` (404 if missing), `GET` (list). Persists through `PicnicRepository`.
- `service/FileSystemStorageService.java` — stores uploaded files on disk under the `upload.dir` property (see `application.properties`, default `uploads/`), renaming each file to a random UUID + original extension to avoid collisions/path traversal, and creates the directory on startup. Each upload also saves an `ImageMetadata` record to Redis.
- `repository/PicnicRepository.java` — Redis storage for `PicnicRecord`: values under `picnic:<id>`, plus a `picnics` set (via `StringRedisTemplate`) used to list all ids.
- `image/ImageMetadata.java`, `image/ImageMetadataRepository.java` — image metadata (id, original name, content type, size, path, upload time) stored in Redis under `image:<id>`. Nothing reads it back yet (`findById`/`deleteById` are unused).
- `config/RedisConfig.java` — `RedisTemplate` beans for `ImageMetadata` and `PicnicRecord`, using a string key serializer and `JacksonJsonRedisSerializer` values.
- `config/WebConfig.java` — maps the `/uploads/**` URL path to the `upload.dir` filesystem location via `WebMvcConfigurer.addResourceHandlers`, so files stored by `FileSystemStorageService` become directly downloadable at the URL returned in the upload response.
- `model/` — domain classes. `PicnicAttributes` is immutable with a builder (`PicnicAttributes.builder().id().lat().lon().build()`; id defaults to a random UUID) and a `from(PicnicAttributeDTO)` factory. `Picnic` holds a name, attributes, description and `photoFilename` (the `ImageMetadata` id of an uploaded image, or null). `PicnicRecord` is the flat, serializable form stored in Redis and returned by the API. The old `PicnicSite`/`PicnicSiteImpl` stubs were removed.
- `dto/` — `PicnicAttributeDTO` (id, lat, lon) and `CreatePicnicRequest` (the POST body: name, lat, lon, description, photoFilename). Both validate lat in -90..90 and lon in -180..180 (negative longitudes are valid, e.g. London). `PicnicAttributeDTO` is currently only used in tests and by `PicnicAttributes.from`.

Key request flow for uploads: `ImageUploadController` → `FileSystemStorageService.store()` (writes file, returns UUID filename) → response includes `url` from `FileSystemStorageService.getFileUrl()`, which is served statically per `WebConfig`'s resource handler mapping.

Key request flow for picnic sites: `PicnicController` → builds `PicnicAttributes`/`Picnic` → `PicnicRecord.from()` → `PicnicRepository.save()` (Redis).

Configuration lives in `src/main/resources/application.properties`: `upload.dir` (storage location), multipart size limits (`spring.servlet.multipart.max-file-size` / `max-request-size`, both 10MB), and `spring.data.redis.host` / `port`.

### Frontend

The frontend lives in a separate project, `../MyPicnicPlaceFrontend` (Vite + TypeScript, Leaflet with marker clustering). It shows picnic sites on a UK map from a hard-coded list and does not call this backend yet.

## Testing notes

- Tests use plain JUnit 5, not always `@SpringBootTest` — e.g. `UploadControllerTest` constructs `FileSystemStorageService` and `ImageUploadController` directly with a JUnit `@TempDir`, and hand-implements `MultipartFile` rather than mocking the framework.
- `MultipartFileFactoryTest` demonstrates loading a classpath resource (`src/main/resources/img/chocksaway.png`) into a `MockMultipartFile` for upload-related tests.
- `PicnicControllerTest` constructs `PicnicController` directly with a Mockito mock of `PicnicRepository` (no Spring context, no Redis) and validates `CreatePicnicRequest` with a plain `Validator`.
- `TestPicnicAttributeDTO` validates DTOs directly against a `jakarta.validation.Validator` built in `@BeforeAll`, without Spring context.
